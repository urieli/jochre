package com.joliciel.jochre.search;

import java.util.Arrays;

import org.apache.lucene.util.BytesRef;

import com.joliciel.jochre.search.alto.AltoString;

public class JochrePayload {
	private BytesRef bytesRef;
	private int left;
	private int top;
	private int width;
	private int height;
	private int pageIndex;
	private int textBlockIndex;
	private int textLineIndex;
	
	public JochrePayload(BytesRef bytesRef) {
		this.bytesRef = bytesRef;
		byte[] bytes = bytesRef.bytes;
		if (bytesRef.length!=12)
			throw new RuntimeException("bytesRef wrong size, should be 12, was " + bytesRef.length + ": " + Arrays.toString(bytes));
		int i = bytesRef.offset;
		this.left = bytes[i+0] * 256 + (bytes[i+1]<0 ? 256 - bytes[i+1] : bytes[i+1]);
		this.top = bytes[i+2] * 256 + (bytes[i+3]<0 ? 256 - bytes[i+3] : bytes[i+3]);
		this.width = bytes[i+4] * 256 + (bytes[i+5]<0 ? 256 - bytes[i+5] : bytes[i+5]);
		this.height = bytes[i+6] * 256 + (bytes[i+7]<0 ? 256 - bytes[i+7] : bytes[i+7]);
		this.pageIndex = bytes[i+8] * 256 + (bytes[i+9]<0 ? 256 - bytes[i+9] : bytes[i+9]);
		this.textBlockIndex = bytes[i+10];
		this.textLineIndex = bytes[i+11];
	}
	
	public JochrePayload(AltoString string) {
		byte[] bytes = new byte[12];
		bytes[0] = (byte) (string.getLeft() / 256);
		bytes[1] = (byte) (string.getLeft() % 256);
		bytes[2] = (byte) (string.getTop() / 256);
		bytes[3] = (byte) (string.getTop() % 256);
		bytes[4] = (byte) (string.getWidth() / 256);
		bytes[5] = (byte) (string.getWidth() % 256);
		bytes[6] = (byte) (string.getHeight() / 256);
		bytes[7] = (byte) (string.getHeight() % 256);
		bytes[8] = (byte) (string.getTextLine().getTextBlock().getPage().getPageIndex() / 256);
		bytes[9] = (byte) (string.getTextLine().getTextBlock().getPage().getPageIndex() % 256);
		bytes[10] = (byte) (string.getTextLine().getTextBlock().getIndex());
		bytes[11] = (byte) (string.getTextLine().getIndex());
		bytesRef = new BytesRef(bytes);
	}
	
	public JochrePayload(int left, int top, int width, int height,
			int pageIndex, int textBlockIndex, int textLineIndex) {
		super();
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		this.pageIndex = pageIndex;
		this.textBlockIndex = textBlockIndex;
		this.textLineIndex = textLineIndex;
	}

	public BytesRef getBytesRef() {
		return bytesRef;
	}

	public int getLeft() {
		return left;
	}

	public int getTop() {
		return top;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getRight() {
		return left + width -1;
	}
	
	public int getBottom() {
		return top + height -1;
	}
	public int getPageIndex() {
		return pageIndex;
	}

	public int getTextBlockIndex() {
		return textBlockIndex;
	}

	public int getTextLineIndex() {
		return textLineIndex;
	}
	
	@Override
	public String toString() {
		return "JochrePayload [left=" + left + ", top=" + top + ", width="
				+ width + ", height=" + height + ", pageIndex=" + pageIndex
				+ ", textBlockIndex=" + textBlockIndex + ", textLineIndex="
				+ textLineIndex + "]";
	}

}
