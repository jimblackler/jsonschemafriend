package net.jimblackler.jsonschemafriend;

import static com.ibm.icu.text.IDNA.CHECK_CONTEXTJ;
import static com.ibm.icu.text.IDNA.CHECK_CONTEXTO;
import static com.ibm.icu.text.IDNA.NONTRANSITIONAL_TO_ASCII;
import static java.net.InetAddress.getByName;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_2019_09;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_3;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_4;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_6;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_7;

import com.damnhandy.uri.template.MalformedUriTemplateException;
import com.damnhandy.uri.template.UriTemplate;
import com.ibm.icu.text.IDNA;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.json.JSONPointer;

public class FormatChecker {
  private static final Collection<String> IDNA_DISALLOWED;
  private static final Pattern RELATIVE_JSON_POINTER_PATTERN = Pattern.compile("^(\\d+)(.*)$");

  static {
    Collection<String> set = new HashSet<>();
    set.add("\u0640");
    set.add("\u07FA");
    set.add("\u302E");
    set.add("\u302F");
    set.add("\u3031");
    set.add("\u3032");
    set.add("\u3033");
    set.add("\u3034");
    set.add("\u3035");
    set.add("\u303B");
    IDNA_DISALLOWED = set;
  }

  static String formatCheck(
      String string, String format, URI metaSchema, RegExPatternSupplier regExPatternSupplier) {
    boolean doAll = false;
    if (metaSchema.equals(DRAFT_2019_09)) {
      doAll = true;
      switch (format) {
        case "uuid":
          try {
            UUID uuid = UUID.fromString(string);
            if (!string.toLowerCase().equals(uuid.toString())) {
              return "Not canonical";
            }
          } catch (IllegalArgumentException e) {
            return e.getMessage();
          }
          break;
      }
    }

    if (doAll || metaSchema.equals(DRAFT_7)) {
      doAll = true;
      switch (format) {
        case "iri":
          try {
            URI uri1 = new URI(string);
            if (!uri1.isAbsolute()) {
              return "Not absolute";
            }
            String authority = uri1.getAuthority();
            if (authority != null
                && InetAddressValidator.getInstance().isValidInet6Address(authority)) {
              return "ipv6 not valid as host in an IRI";
            }
          } catch (URISyntaxException e) {
            return e.getReason();
          }
          break;
        case "idn-email":
          try {
            InternetAddress[] parsed = InternetAddress.parse(string);
            if (parsed.length == 1) {
              parsed[0].validate();
            } else {
              return "Unexpected parse result";
            }
          } catch (AddressException e) {
            return e.getMessage();
          }
          break;
        case "relative-json-pointer":
          Matcher matcher = RELATIVE_JSON_POINTER_PATTERN.matcher(string);
          System.out.println("groupCount " + matcher.groupCount());
          if (!matcher.find() || matcher.groupCount() != 2) {
            return "Relative JSON Pointer invalid";
          }
          String number = matcher.group(1);
          String remain = matcher.group(2);
          if (!"#".equals(remain)) {
            return checkJsonPointer(remain);
          }
          break;
        case "idn-hostname":
          for (int idx = 0; idx < string.length(); idx++) {
            char c = string.charAt(idx);
            if (IDNA_DISALLOWED.contains(String.valueOf(c))) {
              return "Disallowed character " + c;
            }
          }
          StringBuilder sb = new StringBuilder();
          IDNA.Info info = new IDNA.Info();
          IDNA.getUTS46Instance(CHECK_CONTEXTJ | NONTRANSITIONAL_TO_ASCII | CHECK_CONTEXTO)
              .nameToASCII(string, sb, info);
          if (!info.getErrors().isEmpty()) {
            return info.getErrors().toString();
          }
          break;
      }
    }

    if (doAll || metaSchema.equals(DRAFT_6)) {
      doAll = true;
      switch (format) {
        case "iri-reference":
        case "uri-reference":
          try {
            new URI(string);
          } catch (URISyntaxException e) {
            return e.getReason();
          }
          break;
        case "uri-template":
          try {
            UriTemplate.buildFromTemplate(string);
          } catch (MalformedUriTemplateException e) {
            return e.getMessage();
          }
          break;
        case "json-pointer":
          return checkJsonPointer(string);
      }
    }

    if (doAll || metaSchema.equals(DRAFT_3) || metaSchema.equals(DRAFT_4)) {
      switch (format) {
        case "regex":
          try {
            regExPatternSupplier.newPattern(string);
          } catch (InvalidRegexException ex) {
            return ex.getMessage();
          }
          break;
        case "date":
          try {
            DateTimeFormatter.ISO_DATE.parse(string);
          } catch (DateTimeParseException e) {
            return e.getMessage();
          }
          break;
        case "time":
          try {
            DateTimeFormatter.ISO_TIME.parse(string);
          } catch (DateTimeParseException e) {
            return e.getMessage();
          }
          break;
        case "date-time":
          try {
            DateTimeFormatter.ISO_DATE_TIME.parse(string);
          } catch (DateTimeParseException e) {
            return e.getMessage();
          }
          break;
        case "email":
          if (!EmailValidator.getInstance().isValid(string)) {
            return "Did not match";
          }
          break;
        case "uri":
          try {
            if (string.startsWith("//")) {
              return "Protocol-relative";
            }
            URI uri1 = new URI(string);
            if (!metaSchema.equals(MetaSchemaUris.DRAFT_3) && !uri1.isAbsolute()) {
              return "Not absolute";
            }
          } catch (URISyntaxException e) {
            return e.getReason();
          }
          break;
        case "hostname":
        case "host-name":
          if (!DomainValidator.getInstance().isValid(string)) {
            return "Failed DomainValidator";
          }
          break;
        case "ipv6":
          if (!InetAddressValidator.getInstance().isValidInet6Address(string)) {
            return "Failed InetAddressValidator";
          }
          try {
            getByName(string);
          } catch (UnknownHostException e) {
            return e.getMessage();
          }
          break;
        case "ipv4":
        case "ip-address":
          if (!InetAddressValidator.getInstance().isValidInet4Address(string)) {
            return "Failed InetAddressValidator";
          }
          break;
      }
    }

    return null;
  }

  private static String checkJsonPointer(String string) {
    try {
      JSONPointer jsonPointer = new JSONPointer(string);
      String readBack = jsonPointer.toString().replace("\\\\", "\\").replace("\\\"", "\"");
      if (readBack.equals(string)) {
        return null;
      } else {
        return "Not canonical: " + readBack;
      }
    } catch (IllegalArgumentException e) {
      return e.getMessage();
    }
  }
}
