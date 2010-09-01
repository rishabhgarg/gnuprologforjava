/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2010       Daniel Thomas
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA. The text of license can be also found
 * at http://www.gnu.org/copyleft/lgpl.html
 */

/* @Was@ Generated By:JavaCC: Do not edit this line. ASCII_CharStream.java Version 0.7pre3 */
/* Modified by Constantine A. Plotnikov <cap@tassun.math.nsc.ru> 1997-07-30 */

package gnu.prolog.io.parser;

import gnu.prolog.io.parser.gen.CharStream;

/**
 * An implementation of interface CharStream, where the stream is assumed to
 * contain only ASCII characters (without unicode processing).
 * 
 * TODO: add unicode support.
 */

public final class ReaderCharStream implements CharStream
{
	private static final int DEFAULTBUFFERSIZE = 4096;
	private static final int DEFAULTBUFFERINCREMENT = DEFAULTBUFFERSIZE / 2;

	int bufsize;
	int available;
	int tokenBegin;
	public int bufpos = -1;
	private int bufline[];
	private int bufcolumn[];

	private int column = 0;
	private int line = 1;

	private boolean prevCharIsCR = false;
	private boolean prevCharIsLF = false;

	private java.io.Reader reader;

	private char[] buffer;
	private int maxNextCharInd = 0;
	private int inBuf = 0;

	private final void expandBuff(boolean wrapAround)
	{
		char[] newbuffer = new char[bufsize + DEFAULTBUFFERINCREMENT];
		int newbufline[] = new int[bufsize + DEFAULTBUFFERINCREMENT];
		int newbufcolumn[] = new int[bufsize + DEFAULTBUFFERINCREMENT];

		if (wrapAround)
		{
			System.arraycopy(buffer, tokenBegin, newbuffer, 0, bufsize - tokenBegin);
			System.arraycopy(buffer, 0, newbuffer, bufsize - tokenBegin, bufpos);
			buffer = newbuffer;

			System.arraycopy(bufline, tokenBegin, newbufline, 0, bufsize - tokenBegin);
			System.arraycopy(bufline, 0, newbufline, bufsize - tokenBegin, bufpos);
			bufline = newbufline;

			System.arraycopy(bufcolumn, tokenBegin, newbufcolumn, 0, bufsize - tokenBegin);
			System.arraycopy(bufcolumn, 0, newbufcolumn, bufsize - tokenBegin, bufpos);
			bufcolumn = newbufcolumn;

			maxNextCharInd = bufpos += bufsize - tokenBegin;
		}
		else
		{
			System.arraycopy(buffer, tokenBegin, newbuffer, 0, bufsize - tokenBegin);
			buffer = newbuffer;

			System.arraycopy(bufline, tokenBegin, newbufline, 0, bufsize - tokenBegin);
			bufline = newbufline;

			System.arraycopy(bufcolumn, tokenBegin, newbufcolumn, 0, bufsize - tokenBegin);
			bufcolumn = newbufcolumn;

			maxNextCharInd = bufpos -= tokenBegin;
		}

		bufsize += DEFAULTBUFFERINCREMENT;
		available = bufsize;
		tokenBegin = 0;
	}

	private final void fillBuff() throws java.io.IOException
	{
		if (maxNextCharInd == available)
		{
			if (available == bufsize)
			{
				if (tokenBegin > DEFAULTBUFFERINCREMENT)
				{
					bufpos = maxNextCharInd = 0;
					available = tokenBegin;
				}
				else if (tokenBegin < 0)
				{
					bufpos = maxNextCharInd = 0;
				}
				else
				{
					expandBuff(false);
				}
			}
			else if (available > tokenBegin)
			{
				available = bufsize;
			}
			else if (tokenBegin - available < DEFAULTBUFFERINCREMENT)
			{
				expandBuff(true);
			}
			else
			{
				available = tokenBegin;
			}
		}

		int i;
		try
		{
			if ((i = reader.read(buffer, maxNextCharInd, available - maxNextCharInd)) == -1)
			{
				reader.close();
				throw new java.io.IOException();
			}
			else
			{
				maxNextCharInd += i;
			}
			return;
		}
		catch (java.io.IOException e)
		{
			--bufpos;
			backup(0);
			if (tokenBegin == -1)
			{
				tokenBegin = bufpos;
			}
			throw e;
		}
	}

	public final char BeginToken() throws java.io.IOException
	{
		tokenBegin = -1;
		char c = readChar();
		tokenBegin = bufpos;

		return c;
	}

	private final void updateLineColumn(char c)
	{
		column++;

		if (prevCharIsLF)
		{
			prevCharIsLF = false;
			column = 1;
			++line;
		}
		else if (prevCharIsCR)
		{
			prevCharIsCR = false;
			if (c == '\n')
			{
				prevCharIsLF = true;
			}
			else
			{
				column = 1;
				++line;
			}
		}

		switch (c)
		{
			case '\r':
				prevCharIsCR = true;
				break;
			case '\n':
				prevCharIsLF = true;
				break;
			case '\t':
				column += 9 - (column & 07);
				break;
			default:
				break;
		}

		bufline[bufpos] = line;
		bufcolumn[bufpos] = column;
	}

	public final char readChar() throws java.io.IOException
	{
		if (inBuf > 0)
		{
			--inBuf;
			return buffer[bufpos == bufsize - 1 ? (bufpos = 0) : ++bufpos];
		}

		if (++bufpos >= maxNextCharInd)
		{
			fillBuff();
		}

		char c = buffer[bufpos];

		updateLineColumn(c);
		return c;
	}

	/**
	 * @deprecated
	 * @see #getEndColumn
	 */
	@Deprecated
	public final int getColumn()
	{
		return bufcolumn[bufpos];
	}

	/**
	 * @deprecated
	 * @see #getEndLine
	 */
	@Deprecated
	public final int getLine()
	{
		return bufline[bufpos];
	}

	public final int getEndColumn()
	{
		if (bufpos > 0 & bufpos < bufcolumn.length)
		{
			return bufcolumn[bufpos];
		}
		else
		{
			return 0;
		}
	}

	public final int getEndLine()
	{
		if (bufpos > 0 & bufpos < bufline.length)
		{
			return bufline[bufpos];
		}
		else
		{
			return 0;
		}
	}

	public final int getBeginColumn()
	{
		return bufcolumn[tokenBegin];
	}

	public final int getBeginLine()
	{
		return bufline[tokenBegin];
	}

	public final void backup(int amount)
	{

		inBuf += amount;
		if ((bufpos -= amount) < 0)
		{
			bufpos += bufsize;
		}
	}

	public ReaderCharStream(java.io.Reader dstream, int startline, int startcolumn, int buffersize)
	{
		reInit(dstream, startline, startcolumn, buffersize);
	}

	public ReaderCharStream(java.io.Reader dstream, int startline, int startcolumn)
	{
		this(dstream, startline, startcolumn, DEFAULTBUFFERSIZE);
	}

	public void reInit(java.io.Reader dstream, int startline, int startcolumn, int buffersize)
	{
		reader = dstream;
		line = startline;
		column = startcolumn - 1;

		if (buffer == null || buffersize != buffer.length)
		{
			available = bufsize = buffersize;
			buffer = new char[buffersize];
			bufline = new int[buffersize];
			bufcolumn = new int[buffersize];
		}
		prevCharIsLF = prevCharIsCR = false;
		tokenBegin = inBuf = maxNextCharInd = 0;
		bufpos = -1;
	}

	public void reInit(java.io.Reader dstream, int startline, int startcolumn)
	{
		reInit(dstream, startline, startcolumn, DEFAULTBUFFERSIZE);
	}

	public final String GetImage()
	{
		if (bufpos >= tokenBegin)
		{
			return new String(buffer, tokenBegin, bufpos - tokenBegin + 1);
		}
		else
		{
			return new String(buffer, tokenBegin, bufsize - tokenBegin) + new String(buffer, 0, bufpos + 1);
		}
	}

	public final char[] GetSuffix(int len)
	{
		char[] ret = new char[len];

		if (bufpos + 1 >= len)
		{
			System.arraycopy(buffer, bufpos - len + 1, ret, 0, len);
		}
		else
		{
			System.arraycopy(buffer, bufsize - (len - bufpos - 1), ret, 0, len - bufpos - 1);
			System.arraycopy(buffer, 0, ret, len - bufpos - 1, bufpos + 1);
		}

		return ret;
	}

	public void Done()
	{
		buffer = null;
		bufline = null;
		bufcolumn = null;
	}

}
