package com.energyxxer.trident.compiler;

public class TridentProjectProperties {
    public int languageLevel;
    public String anonymousFunctionTemplate;


    public String createAnonymousFunctionName(int index) {
        return anonymousFunctionTemplate.replace("*",String.valueOf(index));
    }
}
