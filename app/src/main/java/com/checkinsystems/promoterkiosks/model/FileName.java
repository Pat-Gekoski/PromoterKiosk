package com.checkinsystems.promoterkiosks.model;

public class FileName {

    String fileName;
    boolean isSelected = false;

    public FileName(){
        fileName = "default string";
    }

    public FileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName(){
        return fileName;
    }

    public void setFileName(String string){
        this.fileName = string;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
