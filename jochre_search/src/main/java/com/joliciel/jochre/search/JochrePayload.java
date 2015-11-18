package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.util.Arrays;

import org.apache.lucene.util.BytesRef;

public class JochrePayload {
	private BytesRef bytesRef;
	
	public JochrePayload(BytesRef bytesRef) {
		this.bytesRef = bytesRef.clone();
		if (bytesRef.length!=12&&bytesRef.length!=20)
			throw new RuntimeException("bytesRef wrong size, should be 12 or 20, was " + bytesRef.length + ": " + Arrays.toString(bytesRef.bytes));
	}
	
	public JochrePayload(JochreToken token) {
		this(token.getRectangle(), token.getSecondaryRectangle(), token.getPageIndex(), token.getParagraphIndex(), token.getRowIndex());
	}
	
	public JochrePayload(Rectangle rect, Rectangle secondaryRect,
			int pageIndex, int paragraphIndex, int rowIndex) {
		byte[] bytes;
		if (secondaryRect==null)
			bytes = new byte[12];
		else
			bytes = new byte[20];
		bytes[0] = (byte) (rect.x / 256);
		bytes[1] = (byte) (rect.x % 256);
		bytes[2] = (byte) (rect.y / 256);
		bytes[3] = (byte) (rect.y % 256);
		bytes[4] = (byte) (rect.width / 256);
		bytes[5] = (byte) (rect.width % 256);
		bytes[6] = (byte) (rect.height / 256);
		bytes[7] = (byte) (rect.height % 256);
		bytes[8] = (byte) (pageIndex / 256);
		bytes[9] = (byte) (pageIndex % 256);
		bytes[10] = (byte) (paragraphIndex);
		bytes[11] = (byte) (rowIndex);
		if (secondaryRect!=null) {
			bytes[12] = (byte) (secondaryRect.x / 256);
			bytes[13] = (byte) (secondaryRect.x % 256);
			bytes[14] = (byte) (secondaryRect.y / 256);
			bytes[15] = (byte) (secondaryRect.y % 256);
			bytes[16] = (byte) (secondaryRect.width / 256);
			bytes[17] = (byte) (secondaryRect.width % 256);
			bytes[18] = (byte) (secondaryRect.height / 256);
			bytes[19] = (byte) (secondaryRect.height % 256);
		}
		bytesRef = new BytesRef(bytes);
	}

	public BytesRef getBytesRef() {
		return bytesRef;
	}

	public Rectangle getRectangle() {
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		int left = bytes[i+0] * 256 + (bytes[i+1]<0 ? 256 + bytes[i+1] : bytes[i+1]);
		int top = bytes[i+2] * 256 + (bytes[i+3]<0 ? 256 + bytes[i+3] : bytes[i+3]);
		int width = bytes[i+4] * 256 + (bytes[i+5]<0 ? 256 + bytes[i+5] : bytes[i+5]);
		int height = bytes[i+6] * 256 + (bytes[i+7]<0 ? 256 + bytes[i+7] : bytes[i+7]);
		Rectangle rect = new Rectangle(left, top, width, height);
		return rect;
	}
	
	public Rectangle getSecondaryRectangle() {
		if (bytesRef.length<20)
			return null;
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		i+=12;
		int left = bytes[i+0] * 256 + (bytes[i+1]<0 ? 256 + bytes[i+1] : bytes[i+1]);
		int top = bytes[i+2] * 256 + (bytes[i+3]<0 ? 256 + bytes[i+3] : bytes[i+3]);
		int width = bytes[i+4] * 256 + (bytes[i+5]<0 ? 256 + bytes[i+5] : bytes[i+5]);
		int height = bytes[i+6] * 256 + (bytes[i+7]<0 ? 256 + bytes[i+7] : bytes[i+7]);
		Rectangle rect = new Rectangle(left, top, width, height);
		return rect;
	}
	
	public int getPageIndex() {
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		return bytes[i+8] * 256 + (bytes[i+9]<0 ? 256 + bytes[i+9] : bytes[i+9]);
	}

	public int getTextBlockIndex() {
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		return bytes[i+10];
	}

	public int getTextLineIndex() {
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		return bytes[i+11];
	}
	
	@Override
	public String toString() {
		return "JochrePayload [rect=" + this.getRectangle() + ", pageIndex=" + this.getPageIndex()
				+ ", textBlockIndex=" + this.getTextBlockIndex() + ", textLineIndex="
				+ this.getTextLineIndex() + ", secondaryRect=" + this.getSecondaryRectangle() + "]";
	}

}
