/**
 * Copyright (c) 2011-2012 Armin Töpfer
 *
 * This file is part of QuasiRecomb.
 *
 * QuasiRecomb is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 *
 * QuasiRecomb is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * QuasiRecomb. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ethz.bsse.quasirecomb.informationholder;

import ch.ethz.bsse.quasirecomb.utils.BitMagic;
import java.util.Arrays;

/**
 * @author Armin Töpfer (armin.toepfer [at] gmail.com)
 */
public class Read {

    private byte[] watsonSequence;
    private int watsonBegin;
    private int watsonEnd;
    private int count;
    private byte[] crickSequence;
    private int crickBegin;
    private int crickEnd = -1;

    public void merge() {
        if (this.watsonEnd < this.crickBegin) {
            return;
        }
        byte[] consensus = new byte[this.crickEnd - this.watsonBegin];
//        if (Globals.getINSTANCE().isDEBUG()) {
//            for (int i = 0; i < BitMagic.getLength(watsonSequence); i++) {
//                System.out.print(BitMagic.getPosition(this.watsonSequence, i));
//            }
//            System.out.println("");
//            for (int i = 0; i < this.crickBegin - this.watsonBegin; i++) {
//                System.out.print(" ");
//            }
//            for (int i = 0; i < BitMagic.getLength(crickSequence); i++) {
//                System.out.print(BitMagic.getPosition(this.crickSequence, i));
//            }
//            System.out.println("");
//        }

        for (int i = 0; i < this.getLength(); i++) {
            consensus[i] = this.getBase(i);
        }
        this.watsonEnd = this.crickEnd;
        this.watsonSequence = BitMagic.pack(consensus);
        this.crickEnd = -1;
        this.crickBegin = 0;
        this.crickSequence = null;
//        for (int i = 0; i < BitMagic.getLength(watsonSequence); i++) {
//            System.out.print(BitMagic.getPosition(this.watsonSequence, i));
//        }
//        System.out.println("\n");
        Globals.getINSTANCE().incMERGED();
    }

    public enum Position {

        WATSON_IN,
        WATSON_HIT,
        WATSON_OUT,
        INSERTION,
        CRICK_IN,
        CRICK_HIT,
        CRICK_OUT,
        ERROR;
    }

    public Read(byte[] sequence, int begin, int end) {
        this.watsonSequence = sequence;
        this.watsonBegin = begin;
        this.watsonEnd = end;
    }

    public Read(byte[] sequence, int begin, int end, byte[] Csequence, int Cbegin, int Cend) {
        this.watsonSequence = sequence;
        this.watsonBegin = begin;
        this.watsonEnd = end;
        setPairedEnd(Csequence, Cbegin, Cend);
        if (end - begin != BitMagic.getLength(sequence)) {
            throw new IllegalAccessError("length problen: watson. Suggested length: " + (end - begin) + ". Actual length: " + BitMagic.getLength(sequence));
        }
        if (Cend - Cbegin != BitMagic.getLength(Csequence)) {
            throw new IllegalAccessError("length problen: crick");
        }
        rearrange();

        merge();
    }

    public Read(byte[] sequence, int begin, int end, int count) {
        this.watsonSequence = sequence;
        this.watsonBegin = begin;
        this.watsonEnd = end;
        this.count = count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getBegin() {
        return this.watsonBegin;
    }

    public void incCount() {
        count++;
    }

    public int getCount() {
        return count;
    }

    public int getInsertSize() {
        return this.crickBegin - this.watsonEnd;
    }

    public Position getPosition(int j) {
        if (j == 0) {
            return Position.WATSON_IN;
        } else if (j < this.watsonEnd - this.watsonBegin) {
            return Position.WATSON_HIT;
        } else if (this.isPaired()) {
            if (j == this.getWatsonLength()) {
                return Position.WATSON_OUT;
            } else if (j > this.getWatsonLength() && j < this.getWatsonLength() + this.getInsertSize()) {
                return Position.INSERTION;
            } else if (j == this.crickBegin - this.watsonBegin) {
                return Position.CRICK_IN;
            } else if (j > this.crickBegin - this.watsonBegin && j < this.crickBegin + this.getCrickLength() - this.watsonBegin) {
                return Position.CRICK_HIT;
            } else if (j == this.crickBegin + this.getCrickLength() - this.watsonBegin) {
                return Position.CRICK_OUT;
            }
        }
        return Position.ERROR;
//        throw new IllegalAccessError("No such sequence space for hit. j=" + j);
    }

    public boolean isHit(int j) {
        if (j < this.getWatsonLength()) {
            return true;
        } else if (this.isPaired() && j >= this.getWatsonLength() && j < this.getWatsonLength() + this.getInsertSize()) {
            return false;
        } else if (this.isPaired() && j >= this.crickBegin - this.watsonBegin && j < this.crickBegin + this.getCrickLength() - this.watsonBegin) {
            return true;
        } else {
            throw new IllegalAccessError("No such sequence space for hit. j=" + j + "\tl=" + (this.crickBegin + this.getCrickLength() - this.watsonBegin));
        }
    }

    public int getLength() {
        if (this.crickSequence != null) {
            return this.crickEnd - this.watsonBegin;
        } else {
            return this.watsonEnd - this.watsonBegin;
        }
    }

    public int getEnd() {
        if (this.crickEnd == -1) {
            return watsonEnd;
        } else {
            return this.crickEnd;
        }
    }

    public byte[] getSequence() {
        return this.watsonSequence;
    }

    public byte getBase(int j) {
        if (j < this.getWatsonLength()) {
            return BitMagic.getPosition(this.watsonSequence, j);
        } else if (this.isPaired() && j >= this.crickBegin - this.watsonBegin && j < this.crickBegin + this.getCrickLength() - this.watsonBegin) {
            return BitMagic.getPosition(this.crickSequence, j - this.getWatsonLength() - this.getInsertSize());
        } else {
//            return -1;
            throw new IllegalAccessError("No such sequence space. j=" + j);
        }
    }

    public byte[] getCrickSequence() {
        return crickSequence;
    }

    public int getCrickLength() {
        return this.crickEnd - this.crickBegin;
    }

    public int getWatsonLength() {
        return this.watsonEnd - this.watsonBegin;
    }

    public boolean isPaired() {
        return this.crickSequence != null;
    }

    public void setPairedEnd(byte[] sequence, int begin, int end) {
        this.crickSequence = sequence;
        this.crickBegin = begin;
        this.crickEnd = end;
    }

    public int getCrickEnd() {
        return crickEnd;
    }

    public int getWatsonEnd() {
        return watsonEnd;
    }

    public int getWatsonBegin() {
        return watsonBegin;
    }

    public int getCrickBegin() {
        return crickBegin;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Arrays.hashCode(this.watsonSequence);
        hash = 29 * hash + Arrays.hashCode(this.crickSequence);
        hash = 29 * hash + this.watsonBegin;
        hash = 29 * hash + this.watsonEnd;
        hash = 29 * hash + this.crickBegin;
        hash = 29 * hash + this.crickEnd;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == this.getClass() && obj.hashCode() == this.hashCode();
    }

    public void rearrange() {
        if (this.watsonBegin > this.crickBegin) {
            int beginTmp = this.watsonBegin;
            int endTmp = this.watsonEnd;
            byte[] seqTmp = this.watsonSequence;
            this.watsonBegin = this.crickBegin;
            this.watsonEnd = this.crickEnd;
            this.watsonSequence = this.crickSequence;
            this.crickBegin = beginTmp;
            this.crickEnd = endTmp;
            this.crickSequence = seqTmp;
        }
    }
}
