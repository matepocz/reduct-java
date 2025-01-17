package org.reduct.common;

public enum ServerURL {

   SERVER_INFO("api/v1/info");

   private final String url;

   ServerURL(String url) {
      this.url = url;
   }

   public String getUrl() {
      return url;
   }
}
