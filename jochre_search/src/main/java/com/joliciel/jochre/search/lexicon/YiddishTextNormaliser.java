package com.joliciel.jochre.search.lexicon;

class YiddishTextNormaliser implements TextNormaliser {

  public YiddishTextNormaliser(String configId) {
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
    text = text.replaceAll("־", "-");

    return text;
  }
}
