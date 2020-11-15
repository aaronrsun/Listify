package com.example.listify.data;

public class ListShare {
    Integer listID;
    String shareWithEmail;
    final ListShare[] other;

    public ListShare(Integer listID, String shareWithEmail, ListShare[] other) {
        this.listID = listID;
        this.shareWithEmail = shareWithEmail;
        this.other = other;
    }

    public ListShare(Integer listID, String shareWithEmail) {
        this.listID = listID;
        this.shareWithEmail = shareWithEmail;
        this.other = null;
    }

    public Integer getListID() {
        return listID;
    }

    public void setListID(Integer listID) {
        this.listID = listID;
    }

    public String getShareWithEmail() {
        return shareWithEmail;
    }

    public void setShareWithEmail(String shareWithEmail) {
        this.shareWithEmail = shareWithEmail;
    }

    public ListShare[] getEntries() {
        return other;
    }
}
