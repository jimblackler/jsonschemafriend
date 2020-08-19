package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONPointer;

public class SchemaStore {
  private final Collection<URI> unbuilt = new HashSet<>();
  private final Map<URI, Schema> built = new HashMap<>();
  private final Map<URI, Object> documentCache = new HashMap<>();
  private final Map<URI, URI> idToPath = new HashMap<>();
  private final Map<URI, URI> refs = new HashMap<>();
  private final Collection<UriRewriter> rewriters = new ArrayList<>();
  private final URI basePointer;

  interface UriRewriter {
    URI rewrite(URI in);
  }

  public SchemaStore() throws GenerationException {
    try {
      basePointer = new URI(null, null, null);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  public void addRewriter(UriRewriter rewriter) {
    rewriters.add(rewriter);
  }

  public void loadBaseObject(Object jsonObject) throws GenerationException {
    storeDocument(basePointer, jsonObject);
    followAndQueue(basePointer);
  }

  Object fetchDocument(URI uri) throws GenerationException {
    for (UriRewriter rewriter : rewriters) {
      uri = rewriter.rewrite(uri);
    }
    if (documentCache.containsKey(uri)) {
      return documentCache.get(uri);
    }
    String content;

    try (Scanner scanner = new Scanner(uri.toURL().openStream(), StandardCharsets.UTF_8)) {
      content = scanner.useDelimiter("\\A").next();
    } catch (IllegalArgumentException | IOException e) {
      throw new GenerationException("Error fetching " + uri, e);
    }
    Object object;
    try {
      object = new JSONArray(content);
    } catch (JSONException e) {
      try {
        object = new JSONObject(content);
      } catch (JSONException e2) {
        throw new GenerationException(e2);
      }
    }
    storeDocument(uri, object);
    return object;
  }

  private void storeDocument(URI uri, Object object) throws GenerationException {
    documentCache.put(uri, object);
    findIds(uri, null);
  }

  private void findIds(URI uri, URI activeId) throws GenerationException {
    Object object = resolve(uri);
    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      Object idObject = jsonObject.opt("$id");
      if (idObject instanceof String) {
        String id = (String) idObject;
        activeId = URI.create(id);
        idToPath.put(activeId, uri);
      }

      Object refObject = jsonObject.opt("$ref");

      if (refObject instanceof String) {
        String ref = (String) refObject;

        URI refUri = URI.create(ref);
        URI uri1;
        if (activeId != null && refUri.getFragment() == null) {
          // See "This URI also serves as the base URI.." in
          // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-8.2.2
          uri1 = activeId.resolve(refUri);
        } else {
          uri1 = uri.resolve(refUri);
        }
        refs.put(uri, uri1);
      }

      Iterator<String> it = jsonObject.keys();
      while (it.hasNext()) {
        String key = it.next();
        findIds(JsonSchemaRef.append(uri, key), activeId);
      }
    } else if (object instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) object;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        findIds(JsonSchemaRef.append(uri, String.valueOf(idx)), activeId);
      }
    }
  }
  public Object resolve(URI uri) throws GenerationException {
    if (idToPath.containsKey(uri)) {
      uri = idToPath.get(uri);
    }
    try {
      URI documentUri = new URI(uri.getScheme(), uri.getSchemeSpecificPart(), null);
      Object object = fetchDocument(documentUri);
      if (uri.getFragment() == null || "/".equals(uri.getFragment())) {
        return object;
      }
      JSONPointer jsonPointer = new JSONPointer("#" + uri.getFragment());
      return jsonPointer.queryFrom(object);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  public URI followAndQueue(URI uri) {
    // URI must be a path and NOT an id.

    if (unbuilt.contains(uri) || built.containsKey(uri)) {
      return uri;
    }
    if (refs.containsKey(uri)) {
      return followAndQueue(refs.get(uri));
    }
    unbuilt.add(uri);
    return uri;
  }

  public void process() throws GenerationException {
    while (!unbuilt.isEmpty()) {
      URI uri = unbuilt.iterator().next();
      System.out.println("Processing " + uri);
      built.put(uri, Schemas.create(this, uri));
      unbuilt.remove(uri);
    }
  }

  public void loadResources(Path resources) throws GenerationException {
    try (Stream<Path> walk = Files.walk(resources)) {
      for (Path path : walk.collect(Collectors.toList())) {
        if (Files.isDirectory(path)) {
          continue;
        }
        followAndQueue(new URI("file", path.toString(), null));
      }
    } catch (UncheckedGenerationException | IOException | URISyntaxException ex) {
      throw new GenerationException(ex);
    }
  }
}
