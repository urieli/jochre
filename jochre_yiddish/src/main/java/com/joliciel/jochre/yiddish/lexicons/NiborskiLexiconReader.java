///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.yiddish.lexicons;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.jochre.lexicon.TextFileLexicon;

public class NiborskiLexiconReader {
  private static final Logger LOG = LoggerFactory.getLogger(NiborskiLexiconReader.class);

  private int maxEntries = 0;
  private Writer variantWriter = null;
  private Set<NiborskiLexicalFormEntry> allVariants = new TreeSet<NiborskiLexicalFormEntry>();
  private Set<String> entries = new TreeSet<String>();
  private Set<NiborskiLexicalFormEntry> partEntries = new TreeSet<NiborskiLexiconReader.NiborskiLexicalFormEntry>();

  public TextFileLexicon read(File file) throws IOException {
    Scanner scanner = new Scanner(file);
    NiborskiLexiconEntry entry = null;
    NiborskiLexiconSubEntry subEntry = null;
    int i = 0;
    boolean skipSubEntry = false;
    boolean skipEntry = false;
    boolean inComment = false;
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine().trim();
      LOG.debug(line);
      if (line.startsWith("<!--")) {
        inComment = true;
      } else if (line.startsWith("-->"))
        inComment = false;

      if (inComment)
        continue;

      if (line.startsWith("<entry>") || line.startsWith("<entry ")) {
        if (maxEntries > 0 && i > maxEntries)
          break;
        i++;

        if (entry != null) {
          scanner.close();
          throw new RuntimeException("Entry not properly ended: " + entry.text);
        }
        entry = new NiborskiLexiconEntry();
        skipEntry = false;
      } else if (line.startsWith("</entry>")) {
        if (!skipEntry) {
          LOG.debug("" + i);
          if (entry == null) {
            scanner.close();
            throw new RuntimeException("Entry not properly started");
          }
          this.addEntry(entry);
          for (NiborskiLexiconSubEntry oneSubEntry : entry.subEntries) {
            if (oneSubEntry.xref.length() > 0 && !oneSubEntry.category.equals("xref")) {
              NiborskiLexiconEntry xrefEntry = new NiborskiLexiconEntry();
              xrefEntry.text = oneSubEntry.xref;
              NiborskiLexiconSubEntry xrefSubEntry = new NiborskiLexiconSubEntry();
              xrefSubEntry.category = oneSubEntry.category;
              xrefSubEntry.gender = oneSubEntry.gender;
              xrefSubEntry.notes = oneSubEntry.notes;
              xrefEntry.subEntries.add(xrefSubEntry);
              this.addEntry(xrefEntry);
            }
          }
        }
        entry = null;
      } else if (line.startsWith("<text>")) {
        int tagLength = "<text>".length();
        entry.text = line.substring(tagLength, line.indexOf('<', tagLength + 1));
        int pronunciationIndex = line.indexOf("<pronunciation>");
        if (pronunciationIndex >= 0) {
          tagLength = "<pronunciation>".length();
          entry.pronunciation = line.substring(pronunciationIndex + tagLength, line.indexOf('<', pronunciationIndex + tagLength + 1));
        }
        int superscriptIndex = line.indexOf("<sup>");
        if (superscriptIndex >= 0) {
          tagLength = "<sup>".length();
          entry.superscript = Integer.parseInt(line.substring(superscriptIndex + tagLength, line.indexOf('<', superscriptIndex + tagLength + 1)));
        }
        if (entry.text.contains("…"))
          skipEntry = true;
      } else if (line.startsWith("<subentry>")) {
        subEntry = new NiborskiLexiconSubEntry();
        skipSubEntry = false;
      } else if (line.startsWith("</subentry>")) {
        if (!skipSubEntry) {
          if (subEntry.lemma != null)
            subEntry.lemma = subEntry.lemma.replaceAll("עַ", "ע");
          entry.subEntries.add(subEntry);
        }
        subEntry = null;
      } else if (line.startsWith("<subentry skip=\"fr\">")) {
        subEntry = new NiborskiLexiconSubEntry();
        skipSubEntry = true;
      } else if (line.startsWith("<subentry skip=\"en\">")) {
        subEntry = new NiborskiLexiconSubEntry();
        skipSubEntry = false;
      } else if (line.startsWith("<category>")) {
        int tagLength = "<category>".length();
        String category = "";
        if (line.indexOf('<', tagLength + 1) >= 0)
          category = line.substring(tagLength, line.indexOf('<', tagLength + 1));
        else
          category = line.substring(tagLength);
        if (category.equals("פּראָנ—אַק/דאַט"))
          category = "פּראָנ—אַק";
        subEntry.category = category;

        if (line.contains("<form")) {
          int typeIndex = line.indexOf("<form type=\"") + 12;
          String type = line.substring(typeIndex, line.indexOf('"', typeIndex + 1));
          int formIndex = line.indexOf('>', typeIndex + 1) + 1;
          if (line.indexOf('<', formIndex + 1) >= 0) {
            String form = line.substring(formIndex, line.indexOf('<', formIndex + 1));
            subEntry.forms.put(type, form);
          } else {
            subEntry.forms.put(type, "");
          }
          int pronunciationIndex = line.indexOf("<pronunciation>");
          if (pronunciationIndex >= 0) {
            tagLength = "<pronunciation>".length();
            subEntry.formPronunciations.put(type,
                line.substring(pronunciationIndex + tagLength, line.indexOf('<', pronunciationIndex + tagLength + 1)));
          }
        }
      } else if (line.startsWith("<noun")) {
        subEntry.category = "noun";
      } else if (line.startsWith("<verb")) {
        subEntry.category = "verb";
      } else if (line.startsWith("<lemma>")) {
        int tagLength = "<lemma>".length();
        subEntry.lemma = line.substring(tagLength, line.indexOf('<', tagLength + 1));
      } else if (line.startsWith("<gender")) {
        int typeIndex = line.indexOf("type=\"") + 6;
        subEntry.gender += line.substring(typeIndex, line.indexOf('"', typeIndex + 1));
      } else if (line.startsWith("<xref")) {
        int xrefIndex = line.indexOf('>', 4) + 1;
        subEntry.xref = line.substring(xrefIndex, line.indexOf('<', xrefIndex + 1));
        if (subEntry.category.length() == 0)
          subEntry.category = "xref";
      } else if (line.startsWith("<form")) {
        int typeIndex = line.indexOf("type=\"") + 6;
        String type = line.substring(typeIndex, line.indexOf('"', typeIndex + 1));
        int formIndex = line.indexOf('>', typeIndex + 1) + 1;
        if (line.indexOf('<', formIndex + 1) >= 0) {
          String form = line.substring(formIndex, line.indexOf('<', formIndex + 1));
          subEntry.forms.put(type, form);
        } else {
          subEntry.forms.put(type, "");
        }
        int pronunciationIndex = line.indexOf("<pronunciation>");
        if (pronunciationIndex >= 0) {
          int tagLength = "<pronunciation>".length();
          subEntry.formPronunciations.put(type,
              line.substring(pronunciationIndex + tagLength, line.indexOf('<', pronunciationIndex + tagLength + 1)));
        }
      } else if (line.startsWith("<note")) {
        int typeIndex = line.indexOf("type=\"") + 6;
        String type = line.substring(typeIndex, line.indexOf('"', typeIndex + 1));
        subEntry.notes.add(type);
      }
    }

    scanner.close();

    Pattern punctuation = Pattern.compile("\\p{Punct}");
    for (NiborskiLexicalFormEntry partEntry : partEntries) {
      String partText = partEntry.text;
      if (!punctuation.matcher(partText).matches()) {
        if (!entries.contains(partText)) {
          allVariants.add(partEntry);
          entries.add(partText);
        }
      }
    }

    if (this.variantWriter != null) {
      for (NiborskiLexicalFormEntry variant : allVariants) {
        variantWriter.write(variant.toString() + "\n");
        variantWriter.flush();
      }
    }

    TextFileLexicon lexicon = new TextFileLexicon();
    for (String word : entries) {
      lexicon.setEntry(word, 1);
    }
    return lexicon;
  }

  private static class NiborskiLexiconEntry {
    public String text;
    public String pronunciation = "";
    public int superscript = 0;
    public List<NiborskiLexiconSubEntry> subEntries = new ArrayList<NiborskiLexiconReader.NiborskiLexiconSubEntry>();
  }

  private static class NiborskiLexiconSubEntry {
    public String category = "";
    public String gender = "";
    public String xref = "";
    public String lemma = null;
    public Map<String, String> forms = new HashMap<String, String>();
    public Map<String, String> formPronunciations = new HashMap<String, String>();
    public List<String> notes = new ArrayList<String>();
  }

  Collection<NiborskiLexicalFormEntry> getForms(String text, String category, String lemma, String morphology, String pronunciation) {
    return this.getForms(text, category, lemma, morphology, pronunciation, "");
  }

  Collection<NiborskiLexicalFormEntry> getForms(String text, String category, String lemma, String morphology, String pronunciation, String attributes) {
    // TODO - encoding: tsvey yudn, pasekh aleph, etc.
    if (text.indexOf('|') >= 0)
      throw new RuntimeException("Expected all | to be gone: " + text + " in " + lemma + ", " + category);

    text = text.replace("'", "");
    text = text.replace('´', '\'');
    text = text.trim();
    lemma = lemma.replace("|", "");
    lemma = lemma.replace("·", "");
    lemma = lemma.trim();

    if (attributes.startsWith(","))
      attributes = attributes.substring(1);

    Set<String> variants = new TreeSet<String>();
    variants.add(text);

    if (text.contains("(")) {
      Set<String> newVariants = new TreeSet<String>();
      boolean haveNewVariant = true;
      while (haveNewVariant) {
        haveNewVariant = false;
        for (String variant : variants) {
          if (variant.contains("(")) {
            haveNewVariant = true;
            int openParenthesis = variant.indexOf('(');
            int closeParenthesis = variant.indexOf(')');
            String newVariant = variant.substring(0, openParenthesis) + variant.substring(closeParenthesis + 1);
            newVariants.add(newVariant);
            newVariant = variant.substring(0, openParenthesis) + variant.substring(openParenthesis + 1, closeParenthesis)
                + variant.substring(closeParenthesis + 1);
            newVariants.add(newVariant);
          } else {
            newVariants.add(variant);
          }
        }
        if (haveNewVariant) {
          variants = newVariants;
          newVariants = new TreeSet<String>();
        }
      }
    }

    List<NiborskiLexicalFormEntry> forms = new ArrayList<NiborskiLexiconReader.NiborskiLexicalFormEntry>();
    for (String variant : variants) {
      NiborskiLexicalFormEntry entry = new NiborskiLexicalFormEntry(variant, category, lemma, morphology, attributes);
      entry.pronunciation = pronunciation;
      forms.add(entry);
    }

    return forms;
  }

  private static class NiborskiLexicalFormEntry implements Comparable<NiborskiLexicalFormEntry> {

    public NiborskiLexicalFormEntry(String text, String category, String lemma, String morphology, String attributes) {
      super();
      this.text = text;
      this.category = category;
      this.lemma = lemma;
      this.morphology = morphology;
      this.attributes = attributes;
    }

    public String text = "";
    public String category = "";
    public String lemma = "";
    public String morphology = "";
    public String attributes = "";
    public String pronunciation = "";

    @Override
    public String toString() {
      return text.trim() + "\t" + category + "\t" + lemma.trim() + "\t" + morphology + "\t" + attributes + "\t" + pronunciation;
    }

    @Override
    public int compareTo(NiborskiLexicalFormEntry o) {
      if (!this.text.equals(o.text))
        return this.text.compareTo(o.text);
      if (!this.category.equals(o.category))
        return this.category.compareTo(o.category);
      if (!this.lemma.equals(o.lemma))
        return this.lemma.compareTo(o.lemma);
      if (!this.morphology.equals(o.morphology))
        return this.morphology.compareTo(o.morphology);
      return this.attributes.compareTo(o.attributes);
    }

  }

  void addEntry(NiborskiLexiconEntry entry) {
    entry.text = entry.text.replaceAll("עַ", "ע");
    List<NiborskiLexicalFormEntry> variants = this.getVariants(entry);
    for (NiborskiLexicalFormEntry variant : variants) {
      allVariants.add(variant);
      entries.add(variant.text);
    }
  }

  List<NiborskiLexicalFormEntry> getVariants(NiborskiLexiconEntry entry) {
    LOG.debug("Entry: " + entry.text);
    if (entry.pronunciation.length() > 0)
      LOG.debug("pronunciation: " + entry.pronunciation);
    for (NiborskiLexiconSubEntry subEntry : entry.subEntries) {
      LOG.debug("Category: " + subEntry.category);
      if (subEntry.gender.length() > 0)
        LOG.debug("Gender: " + subEntry.gender);
      for (Entry<String, String> formEntry : subEntry.forms.entrySet()) {
        LOG.debug("Form " + formEntry.getKey() + ": " + formEntry.getValue());
      }
    }

    List<NiborskiLexicalFormEntry> variants = new ArrayList<NiborskiLexicalFormEntry>();
    String originalText = entry.text.replace("'", "");
    originalText = originalText.replace('״', '"');
    originalText = originalText.replace("*", "");
    if (entry.text.endsWith("'") && originalText.length() < 3)
      originalText += "´";

    Set<String> textVariants = new TreeSet<String>();
    if (originalText.contains("=")) {
      String[] parts = originalText.split("=");
      for (String part : parts) {
        textVariants.add(part.trim());
      }
    } else {
      textVariants.add(originalText);
    }

    if (originalText.contains("(")) {
      Set<String> newTextVariants = new TreeSet<String>();
      boolean haveNewVariant = true;
      while (haveNewVariant) {
        haveNewVariant = false;
        for (String textVariant : textVariants) {
          if (textVariant.contains("(")) {
            haveNewVariant = true;
            int openParenthesis = textVariant.indexOf('(');
            int closeParenthesis = textVariant.indexOf(')');
            String newVariant = YiddishTextUtils
                .getEndForm(textVariant.substring(0, openParenthesis) + textVariant.substring(closeParenthesis + 1));
            newTextVariants.add(newVariant);
            newVariant = textVariant.substring(0, openParenthesis) + textVariant.substring(openParenthesis + 1, closeParenthesis)
                + textVariant.substring(closeParenthesis + 1);
            newTextVariants.add(newVariant);
          }
        }
        if (haveNewVariant) {
          textVariants = newTextVariants;
          newTextVariants = new TreeSet<String>();
        }
      }
    }

    for (String textVariant : textVariants) {
      // TODO - adjective declination in compound forms

      String lemma = textVariant.replace("|", "");
      if (entry.superscript > 0)
        lemma += "_" + entry.superscript;

      String pronunciation = entry.pronunciation;

      for (NiborskiLexiconSubEntry subEntry : entry.subEntries) {
        String text = textVariant;
        if (subEntry.lemma != null) {
          text = subEntry.lemma;
          lemma = subEntry.lemma;
          if (entry.superscript > 0)
            lemma += "_" + entry.superscript;
        }

        if (text.indexOf(' ') >= 0) {
          // add the parts separately
          String[] parts = text.replace("|", "").split(" ");
          for (String part : parts) {
            if (!part.equals("איז") && !part.equals("האָט"))
              partEntries.addAll(this.getForms(part, "xrefPart", part, "", pronunciation, "@partOf(" + lemma + ")"));
          }
        }

        String originalCategory = subEntry.category;
        String[] categories = originalCategory.split("/");

        for (String medemCategory : categories) {
          String category = "";
          if (medemCategory.equals("verb"))
            category = "v";
          else if (medemCategory.equals("noun"))
            category = "nc";
          else if (medemCategory.equals("אַדי"))
            category = "adj";
          else if (medemCategory.equals("אַדי—עפּי"))
            category = "adjAttr";
          else if (medemCategory.equals("פּאָס—אַדי"))
            category = "adjPoss";
          else if (medemCategory.equals("אַדװ"))
            category = "adv";
          else if (medemCategory.equals("אַדי—אַטר"))
            category = "adjPred";
          else if (medemCategory.equals("אַדי—אינװ"))
            category = "adjInvariable";
          else if (medemCategory.equals("אַדי—עפּי—אינװ"))
            category = "adjInvariableAttr";
          else if (medemCategory.equals("אַדי—מצ"))
            category = "adjPlural";
          else if (medemCategory.equals("אַדי—אַטר—מצ"))
            category = "adjPredPlural";
          else if (medemCategory.equals("אַדי—קאָמפּ"))
            category = "adjComp";
          else if (medemCategory.equals("(דאָס)"))
            category = "dos";
          else if (medemCategory.equals("אַרט"))
            category = "det";
          else if (medemCategory.equals("פּרעפּ"))
            category = "prep";
          else if (medemCategory.equals("אינט"))
            category = "int";
          else if (medemCategory.equals("פּאַרטיקל"))
            category = "part";
          else if (medemCategory.equals("xref"))
            category = "xref";
          else if (medemCategory.equals("קאָנ"))
            category = "conj";
          else if (medemCategory.equals("קװ"))
            category = "coverb";
          else if (medemCategory.equals("פּנ"))
            category = "np";
          else if (medemCategory.equals("קאָל"))
            category = "coll";
          else if (medemCategory.equals("טיטל"))
            category = "title";
          else if (medemCategory.equals("פּראָנ"))
            category = "pro";
          else if (medemCategory.equals("פֿר"))
            category = "phrase";
          else if (medemCategory.equals("פּראָנ—רעל"))
            category = "prorel";
          else if (medemCategory.equals("צװ"))
            category = "number";
          else if (medemCategory.equals("הװ"))
            category = "aux";
          else if (medemCategory.equals("װ-אינפֿ"))
            category = "vInf";
          else if (medemCategory.equals("פּראָנ—אַק") || medemCategory.equals("פּרעפּ + אַרט") || medemCategory.equals("פּראָנ—דאַט"))
            category = "skip";
          else
            throw new RuntimeException("Unknown category for text " + entry.text + ": " + medemCategory);

          if (category.equals("v") || category.equals("aux")) {
            this.addVerb(variants, entry, subEntry, category, text, lemma, pronunciation);
            // handle verbs like "efenen"
            if (text.endsWith("ענ|ען")) {
              String alternateText = text.substring(0, text.length() - "ענ|ען".length()) + "נ|ען";
              this.addVerb(variants, entry, subEntry, category, alternateText, lemma, pronunciation);
            }
          } else if (category.equals("nc")) {
            String morphology = subEntry.gender + "s";
            if (subEntry.gender.equals("pl"))
              morphology = "p";

            boolean haveDim = false;
            boolean isDerivedForm = false;
            String attributes = "";
            boolean needsPossessive = false;
            boolean hasPossessive = false;

            for (Entry<String, String> formEntry : subEntry.forms.entrySet()) {
              String key = formEntry.getKey();
              if (key.equals("dimsrc") || key.equals("dim2src")) {
                isDerivedForm = true;
              }
            }

            if (!isDerivedForm) {
              String baseText = text.replace("|", "");
              for (Entry<String, String> formEntry : subEntry.forms.entrySet()) {
                String key = formEntry.getKey();
                String formPronunciation = pronunciation;
                if (subEntry.formPronunciations.containsKey(key))
                  formPronunciation = subEntry.formPronunciations.get(key);

                String originalForm = formEntry.getValue().replace("'", "");
                String[] forms = originalForm.split("/");
                for (String form : forms) {
                  String inflectedForm = "";

                  if (form.equals("—") || form.equals("-")) {
                    inflectedForm = baseText;
                  } else if (form.equals("—ות") || form.equals("—אָים") || form.equals("—ת")) {
                    inflectedForm = baseText.substring(0, baseText.length() - 1) + form.substring(1);
                  } else if (form.equals("—ים") || form.equals("—יות")) {
                    String inflectedBase = YiddishTextUtils.removeEndForm(baseText);
                    if (inflectedBase.endsWith("ה"))
                      inflectedForm = inflectedBase.substring(0, inflectedBase.length() - 1) + form.substring(1);
                    else
                      inflectedForm = inflectedBase + form.substring(1);
                  } else if (form.equals("-ס")) {
                    inflectedForm = YiddishTextUtils.removeEndForm(baseText) + "ס";
                  } else if (form.startsWith("…")) {
                    String firstLetter = form.substring(1, 2);
                    inflectedForm = text.substring(0, baseText.lastIndexOf(firstLetter)) + form.substring(1);
                  } else if (form.endsWith("־")) {
                    inflectedForm = form + baseText.substring(baseText.indexOf('־') + 1);
                  } else if (form.startsWith("־")) {
                    inflectedForm = baseText.substring(0, baseText.indexOf('־')) + form;
                  } else if (form.equals("ן") || form.equals("ען") || form.equals("עס") || form.equals("ס") || form.equals("ם")
                      || form.equals("ים") || form.equals("ות") || form.equals("ין") || form.equals("טע") || form.equals("קע")
                      || form.equals("עך") || form.equals("שע") || form.equals("יכע") || form.equals("ענע") || form.equals("ע")
                      || form.equals("ער")) {
                    inflectedForm = YiddishTextUtils.removeEndForm(baseText) + form;
                  } else {
                    inflectedForm = form;
                  } // derive the form

                  if (key.equals("pl")) {
                    variants.addAll(this.getForms(inflectedForm, category, lemma, subEntry.gender + "p", formPronunciation, attributes));
                  } else if (key.equals("fem")) {
                    variants.addAll(this.getForms(inflectedForm, category, lemma, "fs", formPronunciation, attributes));
                    variants.addAll(this.getForms(YiddishTextUtils.removeEndForm(inflectedForm) + "ס", category, lemma, "fp",
                        formPronunciation, attributes));
                    needsPossessive = true;
                  } else if (key.equals("dim") || key.equals("dim2")) {
                    variants.addAll(this.getForms(inflectedForm, category, lemma, "ns", formPronunciation, attributes + ",@dim"));

                    if (inflectedForm.endsWith("ל")) {
                      variants.addAll(
                          this.getForms(inflectedForm + "עך", category, lemma, "np", formPronunciation, attributes + ",@dim"));
                      variants.addAll(this.getForms(inflectedForm.substring(0, inflectedForm.length() - 1) + "עלע", category, lemma, "ns",
                          formPronunciation, attributes + ",@dim2"));
                      variants.addAll(this.getForms(inflectedForm.substring(0, inflectedForm.length() - 1) + "עלעך", category, lemma,
                          "np", formPronunciation, attributes + ",@dim2"));
                    }
                    haveDim = true;
                  } else if (key.equals("accdat")) {
                    variants.addAll(this.getForms(inflectedForm, category, lemma, subEntry.gender + "s", formPronunciation,
                        attributes + ",@acc,@dat"));
                  } else if (key.equals("dat")) {
                    variants.addAll(
                        this.getForms(inflectedForm, category, lemma, subEntry.gender + "s", formPronunciation, attributes + ",@dat"));
                  } else if (key.equals("pos")) {
                    variants.addAll(
                        this.getForms(inflectedForm, category, lemma, subEntry.gender + "s", formPronunciation, attributes + ",@poss"));
                    hasPossessive = true;
                  } else if (key.equals("indef")) {
                    if (inflectedForm.startsWith("אַ ")) {
                      inflectedForm = inflectedForm.substring("אַ ".length());
                    } else if (inflectedForm.startsWith("אַן ")) {
                      inflectedForm = inflectedForm.substring("אַן ".length());
                    }
                    variants.addAll(this.getForms(inflectedForm, category, lemma, "ns", formPronunciation, "@indef"));
                  } else if (key.equals("supsrc") || key.equals("compsrc")) {
                    // do nothing for now
                  } else {
                    throw new RuntimeException("Unknown form type for " + entry.text + ": " + key);
                  }
                } // next form
              } // next form entry

              boolean declinable = false;
              if (text.contains("|") && text.endsWith("ער")) {
                declinable = true;
                String radical = this.getAdjectiveRadical(text);
                this.addAdjective(variants, "nc", radical, lemma, pronunciation, attributes);
              } else {
                variants.addAll(this.getForms(baseText, category, lemma, morphology, pronunciation));
                if (needsPossessive && !hasPossessive) {
                  String possessiveForm = YiddishTextUtils.removeEndForm(baseText);
                  if (possessiveForm.endsWith("ס") || possessiveForm.endsWith("ש") || possessiveForm.endsWith("צ"))
                    possessiveForm += "עס";
                  else
                    possessiveForm += "ס";
                  variants.addAll(this.getForms(possessiveForm, category, lemma, morphology, pronunciation, "@poss"));
                }
              }

              if (!declinable && !haveDim && text.length() > 2) {
                String root = YiddishTextUtils.removeEndForm(baseText);
                if (root.endsWith("ל"))
                  root += "כ";
                else if (root.endsWith("נ") || root.endsWith("מ"))
                  root += "ד";
                // TODO: are these the right startings for
                // diminutives?
                variants.addAll(this.getForms(root + "ל", category, lemma, "ns", pronunciation, "@dim,@guess"));
                variants.addAll(this.getForms(root + "לעך", category, lemma, "np", pronunciation, "@dim,@guess"));
                if (!root.endsWith("ע") && !root.endsWith("ה")) {
                  variants.addAll(this.getForms(root + "עלע", category, lemma, "ns", pronunciation, "@dim2,@guess"));
                  variants.addAll(this.getForms(root + "עלעך", category, lemma, "np", pronunciation, "@dim2,@guess"));
                } else {
                  variants.addAll(this.getForms(root + "לע", category, lemma, "ns", pronunciation, "@dim2,@guess"));
                }
              }
            } // is this a diminutive of a main form?
          } else if (category.equals("pro") || category.equals("prorel")) {
            // pronoun
            String baseText = text.replace("|", "");

            boolean isNominative = true;
            if (subEntry.forms.containsKey("nom"))
              isNominative = false;

            if (isNominative) {
              String morphology = "3s";
              if (text.equals("איך"))
                morphology = "1s";
              else if (text.equals("דו"))
                morphology = "2s";
              else if (text.equals("ער"))
                morphology = "3ms";
              else if (text.equals("זי"))
                morphology = "3fs";
              else if (text.equals("עס"))
                morphology = "3ns";
              else if (text.equals("מיר"))
                morphology = "1p";
              else if (text.equals("איר"))
                morphology = "2p";
              else if (text.equals("זײ"))
                morphology = "3p";

              variants.addAll(this.getForms(baseText, category, lemma, morphology, "", "@nom"));

              String neuterPredicateIndefiniteForm = null;

              for (Entry<String, String> formEntry : subEntry.forms.entrySet()) {
                String key = formEntry.getKey();
                String formPronunciation = pronunciation;
                if (subEntry.formPronunciations.containsKey(key))
                  formPronunciation = subEntry.formPronunciations.get(key);
                String originalForm = formEntry.getValue().replace("'", "");
                if (originalForm.startsWith("אַיז/האָט")) {
                  originalForm = originalForm.replaceFirst("/", "");
                }
                String[] forms = originalForm.split("/");
                for (String form : forms) {
                  if (key.equals("ntnomacc")) {
                    neuterPredicateIndefiniteForm = form;
                  } else if (key.equals("dat")) {
                    variants.addAll(this.getForms(form, category, lemma, morphology, formPronunciation, "@dat"));
                  } else if (key.equals("acc")) {
                    variants.addAll(this.getForms(form, category, lemma, morphology, formPronunciation, "@acc"));
                  } else if (key.equals("accdat")) {
                    variants.addAll(this.getForms(form, category, lemma, morphology, formPronunciation, "@acc,@dat"));
                  } else if (key.equals("pl")) {
                    if (form.equals("ן") || form.equals("ען") || form.equals("עס") || form.equals("ס") || form.equals("ם")
                        || form.equals("ים") || form.equals("ות") || form.equals("ין") || form.equals("טע") || form.equals("קע")
                        || form.equals("עך") || form.equals("שע") || form.equals("יכע") || form.equals("ענע") || form.equals("ע")
                        || form.equals("ער")) {
                      String inflectedForm = YiddishTextUtils.removeEndForm(baseText) + form;
                      variants.addAll(this.getForms(inflectedForm, category, lemma, "3p", formPronunciation));
                    } else {
                      variants.addAll(this.getForms(form, category, lemma, "3p", formPronunciation));
                    }
                  } else if (key.equals("pos")) {
                    variants.addAll(this.getForms(form, category, lemma, morphology, formPronunciation, "@poss"));
                  } else {
                    throw new RuntimeException("Unknown form type for " + entry.text + ": " + key);
                  }
                }
              }

              if (text.contains("|")) {
                // pronoun with regular declinations
                String radical = this.getAdjectiveRadical(text);
                this.addAdjective(variants, category, radical, lemma, pronunciation, "", null, neuterPredicateIndefiniteForm);
              } else if (neuterPredicateIndefiniteForm != null) {
                variants.addAll(this.getForms(neuterPredicateIndefiniteForm, category, lemma, "3ns", pronunciation, "@nom,@acc"));
              }
            } else {
              LOG.info("Not nominative");
            } // is this in the nominative form?
          } else if (category.equals("adj") || category.equals("adjAttr")) {
            // adjective
            boolean isDerivedForm = false;
            if (subEntry.forms.containsKey("compsrc") || subEntry.forms.containsKey("ptsrc") || subEntry.forms.containsKey("supsrc"))
              isDerivedForm = true;

            if (!isDerivedForm) {
              String attributes = "";
              if (category.equals("adjAttr"))
                attributes = "@attr";

              String predicateForm = text;
              String neuterPredicateIndefiniteForm = null;

              for (Entry<String, String> formEntry : subEntry.forms.entrySet()) {
                String key = formEntry.getKey();
                String formPronunciation = pronunciation;
                if (subEntry.formPronunciations.containsKey(key))
                  formPronunciation = subEntry.formPronunciations.get(key);
                String originalForm = formEntry.getValue().replace("'", "");

                String[] forms = originalForm.split("/");
                for (String form : forms) {
                  if (key.equals("comp")) {
                    this.addAdjective(variants, "adj", form, lemma, formPronunciation, attributes + ",@comp");
                    if (!subEntry.forms.containsKey("sup")) {
                      if (form.endsWith("ער")) {
                        String supBase = form.substring(0, form.length() - 2) + "סט";
                        this.addAdjective(variants, "adj", supBase, lemma, formPronunciation, attributes + ",@sup");
                      }
                    }
                  } else if (key.equals("sup")) {
                    this.addAdjective(variants, "adj", form, lemma, formPronunciation, attributes + ",@sup");
                  } else if (key.equals("attr") || key.equals("infl")) {
                    predicateForm = form;
                  } else if (key.equals("ntnomacc")) {
                    neuterPredicateIndefiniteForm = form;
                  } else {
                    throw new RuntimeException("Unknown form type for " + entry.text + ": " + key);
                  }
                }
              }

              // insert vanishing ayin
              String simpleFormBase = text.replace("עַ", "ע");
              // get rid of declinational ayin
              simpleFormBase = simpleFormBase.replace("·", "");
              String simpleForm = YiddishTextUtils.getEndForm(this.getAdjectiveRadical(simpleFormBase));
              String radical = this.getAdjectiveRadical(predicateForm);
              this.addAdjective(variants, "adj", radical, lemma, pronunciation, attributes, simpleForm, neuterPredicateIndefiniteForm);

              // comparative
              if (!subEntry.forms.containsKey("comp")) {
                String compBase = radical + "ער";
                this.addAdjective(variants, "adj", compBase, lemma, pronunciation, attributes + ",@comp");
              }

              // superlative
              if (!subEntry.forms.containsKey("comp") || !subEntry.forms.containsKey("sup")) {
                String supBase = "";
                if (radical.endsWith("ס"))
                  supBase = radical + "ט";
                else
                  supBase = radical + "סט";
                this.addAdjective(variants, "adj", supBase, lemma, pronunciation, attributes + ",@sup");
              }

              // affectionate
              String affectionateRadical = radical + "ינק";
              this.addAdjective(variants, "adj", affectionateRadical, lemma, pronunciation, attributes + ",@affectionate,@guess");

              // derogatory
              String derogatoryRadical = radical;
              if (radical.endsWith("ל"))
                derogatoryRadical += "עכ";
              else
                derogatoryRadical += "לעכ";

              this.addAdjective(variants, "adj", derogatoryRadical, lemma, pronunciation, attributes + ",@derogatory,@guess");

              // TODO: where are these endings productive?
              variants.addAll(this.getForms(radical + "קײט", "nc", radical + "קײט", "fs", pronunciation, "@guess"));
              if (!radical.endsWith("ע") && !radical.endsWith("ה"))
                variants.addAll(this.getForms(radical + "הײט", "nc", radical + "הײט", "fs", pronunciation, "@guess"));
              // variants.addAll(this.getForms(radical + "ונג",
              // "nc", radical + "ונג", "fs", pronunciation,
              // "@guess"));
              // variants.addAll(this.getForms(radical + "שאַפֿט",
              // "nc", radical + "שאַפֿט", "fs", pronunciation,
              // "@guess"));

            }
          } else if (category.equals("adjPoss")) {
            String radical = this.getAdjectiveRadical(text);
            variants.addAll(this.getForms(text.replace("|", ""), "adj", lemma, "s", pronunciation, "@poss"));
            variants.addAll(this.getForms(radical + "ע", "adj", lemma, "p", pronunciation, "@poss"));

            this.addAdjective(variants, "proPoss", radical, lemma, pronunciation, "@poss");
          } else if (category.equals("adjComp")) {
            this.addAdjective(variants, "adj", text, lemma, pronunciation, "@comp");
          } else if (category.equals("adjPred")) {
            variants.addAll(this.getForms(text, "adj", lemma, "", pronunciation, "@pred"));
          } else if (category.equals("adjInvariable")) {
            variants.addAll(this.getForms(text, "adj", lemma, "", pronunciation, "@invariable"));
          } else if (category.equals("adjInvariableAttr")) {
            variants.addAll(this.getForms(text, "adj", lemma, "", pronunciation, "@invariable,@attr"));
          } else if (category.equals("adjPlural")) {
            variants.addAll(this.getForms(text, "adj", lemma, "p", pronunciation, "@plural"));
          } else if (category.equals("adjPredPlural")) {
            variants.addAll(this.getForms(text, "adj", lemma, "p", pronunciation, "@pred,@plural"));
          } else if (category.equals("det")) {
            variants.addAll(this.getForms(text, "det", lemma, subEntry.gender, pronunciation));
          } else if (category.equals("prep")) {
            variants.addAll(this.getForms(text, "prep", lemma, "", pronunciation));
          } else if (category.equals("part")) {
            variants.addAll(this.getForms(text, "part", lemma, "", pronunciation));
          } else if (category.equals("int")) {
            variants.addAll(this.getForms(text, "int", lemma, "", pronunciation));
          } else if (category.equals("conj")) {
            variants.addAll(this.getForms(text, "conj", lemma, "", pronunciation));
          } else if (category.equals("coverb")) {
            variants.addAll(this.getForms(text, "coverb", lemma, "", pronunciation));
          } else if (category.equals("title")) {
            variants.addAll(this.getForms(text, "title", lemma, "", pronunciation));
          } else if (category.equals("np")) {
            variants.addAll(this.getForms(text, "np", lemma, subEntry.gender, pronunciation));
          } else if (category.equals("coll")) {
            variants.addAll(this.getForms(text, "coll", lemma, subEntry.gender, pronunciation));
          } else if (category.equals("xref")) {
            String xref = subEntry.xref.replace("|", "");
            variants.addAll(this.getForms(text.replace("|", ""), "xref", xref, "", pronunciation));
          } else if (category.equals("skip")) {
            // do nothing (usually handled elsewhere)
          } else if (category.contains("adv")) {
            variants.addAll(this.getForms(text.replace("|", ""), "adv", lemma, "", pronunciation));
          } else if (category.contains("dos")) {
            variants.addAll(this.getForms(text, "nc", lemma, "ns", pronunciation));
          } else if (category.contains("phrase")) {
            variants.addAll(this.getForms(text, "phrase", lemma, "", pronunciation));
          } else if (category.contains("number")) {
            variants.addAll(this.getForms(text, "number", lemma, "", pronunciation));
          } else if (category.contains("vInf")) {
            variants.addAll(this.getForms(text.replace("|", ""), "v", lemma, "W", pronunciation));
          } else {
            throw new RuntimeException("Unknown category for text " + entry.text + ": " + category);
          }
        } // next category
      } // next sub-entry
    } // next base text variant

    LOG.debug("# Variants: ");
    for (NiborskiLexicalFormEntry variant : variants) {
      LOG.debug(variant.toString());
    }

    return variants;
  }

  void addVerb(List<NiborskiLexicalFormEntry> variants, NiborskiLexiconEntry entry, NiborskiLexiconSubEntry subEntry, String category, String text,
      String lemma, String pronunciation) {
    String cat = category;
    text = text.replace('·', '|');
    boolean conjugatedForm = false;
    for (String formKey : subEntry.forms.keySet()) {
      if (formKey.equals("inf")) {
        conjugatedForm = true;
      }
    }

    String attributes = "";
    if (text.endsWith(" זיך")) {
      attributes += ",@refl";
      text = text.substring(0, text.length() - 4);
    }

    boolean compoundForm = false;
    if (text.contains(" ")) {
      compoundForm = true;
      variants.addAll(this.getForms(text.replace("|", ""), cat, lemma, "W", pronunciation, attributes));
    }

    if (!compoundForm && !conjugatedForm) {
      String radical = this.getVerbRadical(text);

      String infinitive = text.replace("|", "");
      String thirdPersonPlural = infinitive;

      boolean isImpersonal = false;
      for (String note : subEntry.notes) {
        if (note.equals("impersonal")) {
          isImpersonal = true;
          attributes += ",@impersonal";
        } else {
          throw new RuntimeException("Unknown note type for " + text + ": " + note);
        }
      }

      String coverb = null;
      if (text.indexOf('|') != text.lastIndexOf('|')) {
        coverb = text.substring(0, text.indexOf('|'));
      }

      boolean haveIrregularPlurals = false;

      for (Entry<String, String> formEntry : subEntry.forms.entrySet()) {
        String key = formEntry.getKey();
        String originalForm = formEntry.getValue().replace("'", "");
        if (originalForm.startsWith("איז/האָט") || originalForm.startsWith("האָט/איז")) {
          originalForm = originalForm.replaceFirst("/", "");
        }

        String formPronunciation = pronunciation;
        if (subEntry.formPronunciations.containsKey(key))
          formPronunciation = subEntry.formPronunciations.get(key);

        String[] forms = originalForm.split("/");
        for (String form : forms) {
          if (key.equals("ppt")) {
            String pastParticiple = text;
            boolean conjugateWithZayn = false;
            boolean conjugateWithHobn = true;
            if (form.startsWith("איז ")) {
              conjugateWithZayn = true;
              conjugateWithHobn = false;
              form = form.substring(4);
            } else if (form.startsWith("האָט ")) {
              conjugateWithZayn = false;
              conjugateWithHobn = true;
              form = form.substring(4);
            } else if (form.startsWith("איזהאָט") || form.startsWith("האָטאיז")) {
              conjugateWithZayn = true;
              conjugateWithHobn = true;
              form = form.substring(7);
            }

            if (form.equals("—")) {
              pastParticiple = infinitive;
            } else if (form.equals("—גע—ט") || form.equals("גע—ט") || form.equals("גע-ט") || form.equals("-גע-ט")) {
              pastParticiple = text.substring(0, text.lastIndexOf('|')) + "ט";
              int coVerbIndex = pastParticiple.indexOf('|');
              if (coVerbIndex < 0)
                pastParticiple = "גע" + pastParticiple;
              else {
                pastParticiple = pastParticiple.substring(0, coVerbIndex) + "גע" + pastParticiple.substring(coVerbIndex + 1);
              }
            } else if (form.equals("(גע)-ט") || form.equals("(גע)—ט")) {
              pastParticiple = text.substring(0, text.lastIndexOf('|')) + "ט";
              String pastParticipleText = pastParticiple.replace("|", "");
              variants.addAll(this.getForms(pastParticipleText, cat, lemma, "K", pronunciation));
              this.addAdjective(variants, "adj", pastParticipleText, pastParticipleText, pronunciation, "@pp");
              int coVerbIndex = pastParticiple.indexOf('|');
              if (coVerbIndex < 0)
                pastParticiple = "גע" + pastParticiple;
              else {
                pastParticiple = pastParticiple.substring(0, coVerbIndex) + "גע" + pastParticiple.substring(coVerbIndex + 1);
              }
            } else if (form.startsWith("—") || form.startsWith("-")) {
              pastParticiple = text.substring(0, text.lastIndexOf('|')) + form.substring(1);
              pastParticiple = pastParticiple.replace("|", "");
            } else {
              if (form.indexOf('—') >= 0 || form.indexOf('/') >= 0 || form.indexOf(',') >= 0 || form.indexOf('-') >= 0) {
                throw new RuntimeException("Unexpected ppt form: " + form);
              } else {
                pastParticiple = form;
              }
            }
            String pastParticipleAttributes = "";
            if (conjugateWithZayn && conjugateWithHobn)
              pastParticipleAttributes = "@zayn,@hobn";
            else if (conjugateWithZayn)
              pastParticipleAttributes = "@zayn";

            variants.addAll(this.getForms(pastParticiple, cat, lemma, "K", formPronunciation, pastParticipleAttributes));
            this.addAdjective(variants, "adj", YiddishTextUtils.removeEndForm(pastParticiple), pastParticiple, pronunciation, "@pp");
            if (pastParticiple.endsWith("ן") && !pastParticiple.endsWith("ען")) {
              // handle געװאָרפֿן = געװאָרפֿענע
              String alternatePP = pastParticiple.substring(0, pastParticiple.length() - 1) + "ען";
              this.addAdjective(variants, "adj", YiddishTextUtils.removeEndForm(alternatePP), pastParticiple, pronunciation, "@pp");
            }

          } else if (key.equals("13pl")) {
            // 1st-3rd person plural form different from infinitive
            variants.addAll(this.getForms(form, cat, lemma, "P13p", formPronunciation));
            thirdPersonPlural = form;

            haveIrregularPlurals = true;
          } else if (key.equals("1sg")) {
            variants.addAll(this.getForms(form, cat, lemma, "P1s", formPronunciation, attributes));
          } else if (key.equals("2sg")) {
            variants.addAll(this.getForms(form, cat, lemma, "P2s", formPronunciation, attributes));
            if (form.endsWith("סט")) {
              variants.addAll(this.getForms(form + "ו", cat, lemma, "P2s", formPronunciation, attributes + ",@P2s+stu"));
            }
          } else if (key.equals("3sg") || key.equals("3sgn")) {
            variants.addAll(this.getForms(form, cat, lemma, "P3s", formPronunciation, attributes));
          } else if (key.equals("1pl")) {
            variants.addAll(this.getForms(form, cat, lemma, "P1p", formPronunciation, attributes));
            haveIrregularPlurals = true;
          } else if (key.equals("2pl")) {
            variants.addAll(this.getForms(form, cat, lemma, "P2p", formPronunciation, attributes));
          } else if (key.equals("3pl")) {
            variants.addAll(this.getForms(form, cat, lemma, "P3p", formPronunciation, attributes));
            thirdPersonPlural = form;
            haveIrregularPlurals = true;
          } else if (key.equals("prespt")) {
            variants.addAll(this.getForms(form, cat, lemma, "G", formPronunciation, attributes));
            this.addAdjective(variants, "adj", YiddishTextUtils.removeEndForm(form), form, pronunciation, "");
          } else if (key.equals("impsg")) {
            variants.addAll(this.getForms(form, cat, lemma, "Y2s", formPronunciation, attributes));
          } else if (key.equals("imppl")) {
            variants.addAll(this.getForms(form, cat, lemma, "Y2p", formPronunciation, attributes));
          } else {
            throw new RuntimeException("Unknown form type for " + entry.text + ": " + key);
          }
        } // next form
      } // next key

      if (haveIrregularPlurals) {
        variants.addAll(this.getForms(infinitive, cat, lemma, "W", pronunciation, attributes));
      } else {
        variants.addAll(this.getForms(infinitive, cat, lemma, "WP13p", pronunciation, attributes));
      }

      String firstPersonForm = this.getFirstPersonForm(text);

      if (coverb == null) {
        if (isImpersonal) {
          boolean needP3s = !subEntry.forms.containsKey("3sg") && !subEntry.forms.containsKey("3sgn");
          if (needP3s) {
            if (radical.endsWith("ט")) {
              variants.addAll(this.getForms(firstPersonForm, cat, lemma, "P3s", pronunciation, attributes));
            } else {
              variants.addAll(this.getForms(radical + "ט", cat, lemma, "P3s", pronunciation, attributes));
            }
          }

        } else {
          boolean needP1s = !subEntry.forms.containsKey("1sg");
          boolean needP2s = !subEntry.forms.containsKey("2sg");
          boolean needP3s = !subEntry.forms.containsKey("3sg") && !subEntry.forms.containsKey("3sgn");
          boolean needP2p = !subEntry.forms.containsKey("2pl");
          boolean needY2s = !subEntry.forms.containsKey("impsg");
          boolean needY2p = !subEntry.forms.containsKey("imppl");

          if (radical.endsWith("ט")) {
            if (needP1s || needP3s || needP2p || needY2s || needY2p) {
              String morphology = "";
              if (needP1s || needP3s || needP2p)
                morphology += "P";
              if (needP1s)
                morphology += "1";
              if (needP3s)
                morphology += "3";
              if (needP1s || needP3s)
                morphology += "s";
              if (needP2p)
                morphology += "2p";
              if (needY2s || needY2p)
                morphology += "Y";
              if (needY2s)
                morphology += "2s";
              if (needY2p)
                morphology += "2p";
              variants.addAll(this.getForms(firstPersonForm, cat, lemma, morphology, pronunciation));
            }
            if (needP2s) {
              variants.addAll(this.getForms(radical + "סט", cat, lemma, "P2s", pronunciation));
              variants.addAll(this.getForms(radical + "סטו", cat, lemma, "P2s", pronunciation, attributes + ",@P2s+stu"));
            }
          } else if (radical.endsWith("ס")) {
            if (needP1s || needP2s || needY2s) {
              String morphology = "";
              if (needP1s || needP2s)
                morphology += "P";
              if (needP1s)
                morphology += "1";
              if (needP2s)
                morphology += "2";
              if (needP1s || needP2s)
                morphology += "s";
              if (needY2s)
                morphology += "Y2s";
              variants.addAll(this.getForms(firstPersonForm, cat, lemma, morphology, pronunciation));
            }
            if (needP2s || needP3s || needP2p || needY2p) {
              String morphology = "";
              if (needP2s || needP3s || needP2p)
                morphology += "P";
              if (needP2s)
                morphology += "2";
              if (needP3s)
                morphology += "3";
              if (needP2s || needP3s)
                morphology += "s";
              if (needP2p)
                morphology += "2p";
              if (needY2p)
                morphology += "Y2p";
              variants.addAll(this.getForms(radical + "ט", cat, lemma, morphology, pronunciation));
              if (needP2s) {
                variants.addAll(this.getForms(radical + "טו", cat, lemma, "P2s", pronunciation, attributes + ",@P2s+stu"));
              }
            }
          } else {
            if (needP1s || needY2s) {
              String morphology = "";
              if (needP1s)
                morphology += "P1s";
              if (needY2s)
                morphology += "Y2s";
              variants.addAll(this.getForms(firstPersonForm, cat, lemma, morphology, pronunciation));
            }
            if (needP3s || needP2p || needY2p) {
              String morphology = "";
              if (needP3s || needP2p)
                morphology += "P";
              if (needP3s)
                morphology += "3s";
              if (needP2p)
                morphology += "2p";
              if (needY2p)
                morphology += "Y2p";
              variants.addAll(this.getForms(radical + "ט", cat, lemma, morphology, pronunciation));
            }
            if (needP2s) {
              variants.addAll(this.getForms(radical + "סט", cat, lemma, "P2s", pronunciation));
              variants.addAll(this.getForms(radical + "סטו", cat, lemma, "P2s", pronunciation, attributes + ",@P2s+stu"));
            }
          }
        } // is it an impersonal verb?
      } // have a coverb? If yes, no conjugated forms required

      if (coverb != null) {
        // with co-verb
        String tsuForm = coverb + "צו" + text.substring(text.indexOf('|') + 1);
        tsuForm = tsuForm.replace("|", "");
        variants.addAll(this.getForms(tsuForm, category, lemma, "W", pronunciation, "@tsu"));
      }

      if (!subEntry.forms.containsKey("prespt")) {
        String presentParticiple = YiddishTextUtils.removeEndForm(thirdPersonPlural) + "דיק";
        variants.addAll(this.getForms(presentParticiple, cat, lemma, "G", pronunciation));
        this.addAdjective(variants, "adj", presentParticiple, presentParticiple, pronunciation, "@prp");
      }

      if (radical.endsWith("ע")) {
        variants.addAll(this.getForms(radical + "ר", "nc", radical + "ער", "ms", pronunciation, "@guess"));
        variants.addAll(this.getForms(radical + "רס", "nc", radical + "ער", "mp", pronunciation, "@guess"));
      } else {
        variants.addAll(this.getForms(radical + "ער", "nc", radical + "ער", "ms", pronunciation, "@guess"));
        variants.addAll(this.getForms(radical + "ערס", "nc", radical + "ער", "mp", pronunciation, "@guess"));
      }
    } // is this an infinitive?
  }

  void addAdjective(List<NiborskiLexicalFormEntry> variants, String category, String radical, String lemma, String pronunciation, String attributes) {
    this.addAdjective(variants, category, radical, lemma, pronunciation, attributes, null, null);
  }

  void addAdjective(List<NiborskiLexicalFormEntry> variants, String category, String radical, String lemma, String pronunciation, String attributes,
      String simpleForm, String neuterPredicateIndefiniteForm) {
    // adjective morphology
    // N = attributive nominative
    // A = attributive accusative
    // D = attributive dative
    // I = attributive nominative neuter preceded by an indefinite article
    // P = predicative
    // J = predicative neuter preceded by an indefinite article
    // m = masculine (singular)
    // f = feminine (singular)
    // n = neuter (singular)
    // p = plural
    if (simpleForm == null)
      simpleForm = YiddishTextUtils.getEndForm(radical);
    if (neuterPredicateIndefiniteForm == null)
      neuterPredicateIndefiniteForm = radical + "ס";

    if (attributes.contains("@attr")) {
      variants.addAll(this.getForms(simpleForm, category, lemma, "In", pronunciation, attributes));
    } else {
      variants.addAll(this.getForms(simpleForm, category, lemma, "InPmfnp", pronunciation, attributes));
      variants.addAll(this.getForms(neuterPredicateIndefiniteForm, category, lemma, "Jn", pronunciation, attributes));
    }

    // if ends with ayin, don't double it
    String noAyinRadical = radical;
    if (radical.endsWith("ע"))
      noAyinRadical = radical.substring(0, radical.length() - 1);

    variants.addAll(this.getForms(noAyinRadical + "ער", category, lemma, "NmDf", pronunciation, attributes));
    variants.addAll(this.getForms(noAyinRadical + "ע", category, lemma, "NfnpAfnpDp", pronunciation, attributes));
    if (radical.equals("נײַ"))
      variants.addAll(this.getForms(radical + "עם", category, lemma, "AmDmn", pronunciation, attributes));
    else if (YiddishTextUtils.endsWithVowel(noAyinRadical))
      variants.addAll(this.getForms(noAyinRadical + "ען", category, lemma, "AmDmn", pronunciation, attributes));
    else if (noAyinRadical.endsWith("נ"))
      variants.addAll(this.getForms(noAyinRadical + "עם", category, lemma, "AmDmn", pronunciation, attributes));
    else if (noAyinRadical.endsWith("מ") || noAyinRadical.endsWith("נג")) {
      variants.addAll(this.getForms(noAyinRadical + "ען", category, lemma, "AmDmn", pronunciation, attributes));
      if (!radical.endsWith("ע"))
        variants.addAll(this.getForms(radical + "ן", category, lemma, "AmDmn", pronunciation, attributes));
    } else
      variants.addAll(this.getForms(radical + "ן", category, lemma, "AmDmn", pronunciation, attributes));
  }

  String getVerbRadical(String lemma) {
    String radical = lemma;
    if (lemma.indexOf('|') >= 0)
      radical = lemma.substring(0, lemma.lastIndexOf('|')).replace("|", "");
    else {
      if (lemma.endsWith("ן"))
        radical = lemma.substring(0, lemma.length() - 1);
      else
        radical = lemma;
    }
    radical = YiddishTextUtils.removeEndForm(radical);

    return radical;
  }

  String getAdjectiveRadical(String lemma) {
    String radical = lemma;
    int verticalBarIndex = lemma.lastIndexOf('|');
    if (verticalBarIndex >= 0)
      radical = lemma.substring(0, lemma.lastIndexOf('|')).replace("|", "");
    radical = YiddishTextUtils.removeEndForm(radical);
    radical = radical.replace('·', 'ע');
    radical = radical.replace("עַ", "");
    return radical;
  }

  String getFirstPersonForm(String infinitive) {
    String firstPersonForm = infinitive;
    int verticalBarIndex = infinitive.lastIndexOf('|');
    if (verticalBarIndex >= 0)
      firstPersonForm = infinitive.substring(0, infinitive.lastIndexOf('|')).replace("|", "");
    firstPersonForm = YiddishTextUtils.getEndForm(firstPersonForm);
    return firstPersonForm;
  }

  public Writer getVariantWriter() {
    return variantWriter;
  }

  public void setVariantWriter(Writer variantWriter) {
    this.variantWriter = variantWriter;
  }

  public static void main(String[] args) throws Exception {
    long startTime = (new Date()).getTime();
    try {
      String command = args[0];
      if (command.equals("load")) {
        NiborskiLexiconReader reader = new NiborskiLexiconReader();
        File file = new File(args[1]);
        Writer variantWriter = null;
        if (args.length > 2) {
          File variantFile = new File(args[2]);
          variantFile.getParentFile().mkdirs();
          variantFile.delete();
          variantWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(variantFile, true), "UTF8"));

        }
        reader.setVariantWriter(variantWriter);

        try {
          TextFileLexicon lexicon = reader.read(file);
          if (args.length > 3) {
            File lexiconFile = new File(args[3]);
            lexicon.serialize(lexiconFile);
          }
        } finally {
          if (variantWriter != null)
            variantWriter.close();
        }
      } else if (command.equals("deserialise")) {
        File memoryBaseFile = new File(args[1]);
        TextFileLexicon lexicon = TextFileLexicon.deserialize(memoryBaseFile);
        String[] words = new String[] { "אײגל", "אױגל" };
        for (String word : words)
          LOG.debug("Have entry " + word + ": " + lexicon.getFrequency(word));
      } else {
        throw new RuntimeException("Unknown command: " + command);
      }

    } finally {
      long endTime = (new Date()).getTime() - startTime;
      LOG.debug("Total runtime: " + ((double) endTime / 1000) + " seconds");
    }
  }

}
