package org.idio.wikipedia.dumps;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.hadoop.io.WritableUtils;

import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;


/**
 * A page from Wikipedia.
 */
public abstract class WikipediaPage {
  public String page;
  protected String title;

  private WikiModel wikiModel;
  private PlainTextConverter textConverter;

  /**
   * Creates an empty <code>WikipediaPage</code> object.
   */
  public WikipediaPage() {
    wikiModel = new WikiModel("", "");
    textConverter = new PlainTextConverter();
  }

  // Explicitly remove <ref>...</ref>, because there are screwy things like this:
  // <ref>[http://www.interieur.org/<!-- Bot generated title -->]</ref>
  // where "http://www.interieur.org/<!--" gets interpreted as the URL by
  // Bliki in conversion to text
  private static final Pattern REF = Pattern.compile("<ref>.*?</ref>");

  private static final Pattern LANG_LINKS = Pattern.compile("\\[\\[[a-z\\-]+:[^\\]]+\\]\\]");
  private static final Pattern DOUBLE_CURLY = Pattern.compile("\\{\\{.*?\\}\\}");

  private static final Pattern URL = Pattern.compile("http://[^ <]+"); // Note, don't capture
  // possible HTML tag

  private static final Pattern HTML_TAG = Pattern.compile("<[^!][^>]*>"); // Note, don't capture
  // comments
  private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

  /**
   * Returns the contents of this page (title + text).
   */
  public String getContent() {
    String s = page;

    // Bliki doesn't seem to properly handle inter-language links, so remove manually.
    s = LANG_LINKS.matcher(s).replaceAll(" ");

    wikiModel.setUp();
    s = getTitle() + "\n" + wikiModel.render(textConverter, s);
    wikiModel.tearDown();

    // The way the some entities are encoded, we have to unescape twice.
    s = StringEscapeUtils.unescapeHtml(StringEscapeUtils.unescapeHtml(s));

    s = REF.matcher(s).replaceAll(" ");
    s = HTML_COMMENT.matcher(s).replaceAll(" ");

    // Sometimes, URL bumps up against comments e.g., <!-- http://foo.com/-->
    // Therefore, we want to remove the comment first; otherwise the URL pattern might eat up
    // the comment terminator.
    s = URL.matcher(s).replaceAll(" ");
    s = DOUBLE_CURLY.matcher(s).replaceAll(" ");
    s = HTML_TAG.matcher(s).replaceAll(" ");

    return s;
  }

  /**
   * Returns the title of this page.
   */
  public String getTitle() {
    return title;
  }

  public static class Link {
    private String anchor;
    private String target;

    private Link(String anchor, String target) {
      this.anchor = anchor;
      this.target = target;
    }

    public String getAnchorText() {
      return anchor;
    }

    public String getTarget() {
      return target;
    }

    public String toString() {
      return String.format("[target: %s, anchor: %s]", target, anchor);
    }
  }

  public List<Link> extractLinks() {
    int start = 0;
    List<Link> links = Lists.newArrayList();

    while (true) {
      start = page.indexOf("[[", start);

      if (start < 0) {
        break;
      }

      int end = page.indexOf("]]", start);

      if (end < 0) {
        break;
      }

      String text = page.substring(start + 2, end);
      String anchor = null;

      // skip empty links
      if (text.length() == 0) {
        start = end + 1;
        continue;
      }

      // skip special links
      if (text.indexOf(":") != -1) {
        start = end + 1;
        continue;
      }

      // if there is anchor text, get only article title
      int a;
      if ((a = text.indexOf("|")) != -1) {
        anchor = text.substring(a + 1, text.length());
        text = text.substring(0, a);
      }

      if ((a = text.indexOf("#")) != -1) {
        text = text.substring(0, a);
      }

      // ignore article-internal links, e.g., [[#section|here]]
      if (text.length() == 0) {
        start = end + 1;
        continue;
      }

      if (anchor == null) {
        anchor = text;
      }
      links.add(new Link(anchor, text));

      start = end + 1;
    }

    return links;
  }

  /**
   * Reads a raw XML string into a <code>WikipediaPage</code> object.
   *
   * @param page the <code>WikipediaPage</code> object
   * @param s    raw XML string
   */
  public static String readPage(WikipediaPage page, String s) {
    page.page = s;
    return page.getContent();
  }

}
