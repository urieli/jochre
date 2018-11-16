package com.joliciel.jochre.search;

import java.io.IOException;
import java.io.InputStream;

public class UnclosableInputStream extends InputStream {
  private InputStream wrappedStream;
  
  public UnclosableInputStream(InputStream wrappedStream) {
    super();
    this.wrappedStream = wrappedStream;
  }

  @Override
  public int read(byte[] b) throws IOException {
    return wrappedStream.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return wrappedStream.read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return wrappedStream.skip(n);
  }

  @Override
  public int available() throws IOException {
    return wrappedStream.available();
  }

  @Override
  public void close() throws IOException {
    // do nothing
  }
  
  public void reallyClose() throws IOException {
    wrappedStream.close();
  }

  @Override
  public synchronized void mark(int readlimit) {
    wrappedStream.mark(readlimit);
  }

  @Override
  public synchronized void reset() throws IOException {
    wrappedStream.reset();
  }

  @Override
  public boolean markSupported() {
    return wrappedStream.markSupported();
  }

  @Override
  public int read() throws IOException {
    return wrappedStream.read();
  }
}
