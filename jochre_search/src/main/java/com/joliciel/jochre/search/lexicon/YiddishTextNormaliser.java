package com.joliciel.jochre.search.lexicon;

import com.joliciel.jochre.search.JochreSearchConfig;

class YiddishTextNormaliser implements TextNormaliser {

  public YiddishTextNormaliser(JochreSearchConfig config) {
  }

  @Override
  public String normalise(String text) {
    // double-character fixes
    text = text.replaceAll("[֑֖֛֢֣֤֥֦֧֪֚֭֮֒֓֔֕֗֘֙֜֝֞֟֠֡֨֩֫֬֯]", "");
    text = text.replaceAll("[ְֱֲֳִֵֶַָֹֺֻּֽֿׁׂׅׄ]", "");
    text = text.replaceAll("װ", "וו");
    text = text.replaceAll("ױ", "וי");
    text = text.replaceAll("[ײײַ]", "יי");
    text = text.replaceAll("[ﭏאָﬞאַאָ]", "א");
    text = text.replaceAll("יִ", "י");
    text = text.replaceAll("וּ", "ו");
    text = text.replaceAll("כֿ", "כ");
    text = text.replaceAll("[בֿבּ]", "ב");
    text = text.replaceAll("[כֿכּ]", "כ");
    text = text.replaceAll("[שׁשׂ]", "ש");
    text = text.replaceAll("[תּ]", "ת");

    return text;
  }
}
