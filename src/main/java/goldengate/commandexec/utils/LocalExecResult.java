/**
   This file is part of GoldenGate Project (named also GoldenGate or GG).

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All GoldenGate Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   GoldenGate is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with GoldenGate .  If not, see <http://www.gnu.org/licenses/>.
 */
package goldengate.commandexec.utils;

/**
 * Message Result for an Execution
 *
 * @author Frederic Bregier
 *
 */
public class LocalExecResult {
    public int status;
    public boolean isSuccess;
    public Exception exception;
    public String result;
    /**
     * @param status
     * @param exception
     * @param result
     */
    public LocalExecResult(boolean isSuccess, int status, Exception exception, String result) {
        this.isSuccess = isSuccess;
        this.status = status;
        this.exception = exception;
        this.result = result;
    }

    /**
     * Constructor from a pre-existing LocalExecResult
     * @param localExecResult
     */
    public LocalExecResult(LocalExecResult localExecResult) {
        this.isSuccess = localExecResult.isSuccess;
        this.status = localExecResult.status;
        this.exception = localExecResult.exception;
        this.result = localExecResult.result;
    }
    /**
     * Set the values from a LocalExecResult (pointer copy)
     * @param localExecResult
     */
    public void set(LocalExecResult localExecResult) {
        this.isSuccess = localExecResult.isSuccess;
        this.status = localExecResult.status;
        this.exception = localExecResult.exception;
        this.result = localExecResult.result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Status: "+status+" Output: "+result+(exception != null ? "\nError: "+exception.getMessage():"");
    }

}
