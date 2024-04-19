package cn;

import java.util.List;

public class Message {

    private Boolean sent;
    private List<String> otherUsernames;
    private String fileName;
    private String content;
    private String sender;



    public Message(Boolean sent, String filename, String sender, String content){
        this.sent=sent;
        //this.otherUsernames=otherUsernames;
        this.fileName=filename;
        this.sender=sender;
        this.content=content;

    }

    public Boolean getSent(){
        return sent;
    }
    public void setSent(Boolean sent) {
        this.sent = sent;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getOtherUsername() {
        return otherUsernames;
    }

    public void setOtherUsername(List<String> otherUsername) {
        this.otherUsernames = otherUsername;
    }

    public String getFileName() {
        return fileName;
    }
    public String toString(){
        return this.sent+","+ this.content + ","+this.otherUsernames;
    }
}
