package com.joliciel.jochre.search;

import java.awt.Rectangle;
import java.util.Arrays;

import org.apache.lucene.util.BytesRef;

public class JochrePayload {
	private BytesRef bytesRef;
	
	public JochrePayload(BytesRef bytesRef) {
		this.bytesRef = BytesRef.deepCopyOf(bytesRef);
		if (bytesRef.length!=14&&bytesRef.length!=22)
			throw new RuntimeException("bytesRef wrong size, should be 14 or 22, was " + bytesRef.length + ": " + Arrays.toString(bytesRef.bytes));
	}
	
	public JochrePayload(JochreToken token) {
		this(token.getRectangle(), token.getSecondaryRectangle(), token.getPageIndex(), token.getParagraphIndex(), token.getRowIndex());
	}
	
	public JochrePayload(Rectangle rect, Rectangle secondaryRect,
			int pageIndex, int paragraphIndex, int rowIndex) {
		byte[] bytes;
		if (secondaryRect==null)
			bytes = new byte[14];
		else
			bytes = new byte[22];
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
		bytes[10] = (byte) (paragraphIndex / 256);
		bytes[11] = (byte) (paragraphIndex % 256);
		bytes[12] = (byte) (rowIndex / 256);
		bytes[13] = (byte) (rowIndex % 256);
		if (secondaryRect!=null) {
			bytes[14] = (byte) (secondaryRect.x / 256);
			bytes[15] = (byte) (secondaryRect.x % 256);
			bytes[16] = (byte) (secondaryRect.y / 256);
			bytes[17] = (byte) (secondaryRect.y % 256);
			bytes[18] = (byte) (secondaryRect.width / 256);
			bytes[19] = (byte) (secondaryRect.width % 256);
			bytes[20] = (byte) (secondaryRect.height / 256);
			bytes[21] = (byte) (secondaryRect.height % 256);
		}
		bytesRef = new BytesRef(bytes);
	}

	public BytesRef getBytesRef() {
		return bytesRef;
	}

	public Rectangle getRectangle() {
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		int left = (bytes[i+0]<0 ? 256 + bytes[i+0] : bytes[i+0]) * 256 + (bytes[i+1]<0 ? 256 + bytes[i+1] : bytes[i+1]);
		int top = (bytes[i+2]<0 ? 256 + bytes[i+2] : bytes[i+2]) * 256 + (bytes[i+3]<0 ? 256 + bytes[i+3] : bytes[i+3]);
		int width = (bytes[i+4]<0 ? 256 + bytes[i+4] : bytes[i+4]) * 256 + (bytes[i+5]<0 ? 256 + bytes[i+5] : bytes[i+5]);
		int height = (bytes[i+6]<0 ? 256 + bytes[i+6] : bytes[i+6]) * 256 + (bytes[i+7]<0 ? 256 + bytes[i+7] : bytes[i+7]);
		Rectangle rect = new Rectangle(left, top, width, height);
		return rect;
	}
	
	public Rectangle getSecondaryRectangle() {
		if (bytesRef.length<20)
			return null;
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		i+=14;
		int left = (bytes[i+0]<0 ? 256 + bytes[i+0] : bytes[i+0]) * 256 + (bytes[i+1]<0 ? 256 + bytes[i+1] : bytes[i+1]);
		int top = (bytes[i+2]<0 ? 256 + bytes[i+2] : bytes[i+2]) * 256 + (bytes[i+3]<0 ? 256 + bytes[i+3] : bytes[i+3]);
		int width = (bytes[i+4]<0 ? 256 + bytes[i+4] : bytes[i+4]) * 256 + (bytes[i+5]<0 ? 256 + bytes[i+5] : bytes[i+5]);
		int height = (bytes[i+6]<0 ? 256 + bytes[i+6] : bytes[i+6]) * 256 + (bytes[i+7]<0 ? 256 + bytes[i+7] : bytes[i+7]);
		Rectangle rect = new Rectangle(left, top, width, height);
		return rect;
	}
	
	public int getPageIndex() {
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		return (bytes[i+8]<0 ? 256 + bytes[i+8] : bytes[i+8]) * 256 + (bytes[i+9]<0 ? 256 + bytes[i+9] : bytes[i+9]);
	}

	public int getParagraphIndex() {
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		return (bytes[i+10]<0 ? 256 + bytes[i+10] : bytes[i+10]) * 256 + (bytes[i+11]<0 ? 256 + bytes[i+11] : bytes[i+11]);
	}

	public int getRowIndex() {
		byte[] bytes = bytesRef.bytes;
		int i = bytesRef.offset;
		return (bytes[i+12]<0 ? 256 + bytes[i+12] : bytes[i+12]) * 256 + (bytes[i+13]<0 ? 256 + bytes[i+13] : bytes[i+13]);
	}
	
	@Override
	public String toString() {
		return "JochrePayload [rect=" + this.getRectangle() + ", pageIndex=" + this.getPageIndex()
				+ ", paragraphIndex=" + this.getParagraphIndex() + ", rowIndex="
				+ this.getRowIndex() + ", secondaryRect=" + this.getSecondaryRectangle() + "]";
	}

}
