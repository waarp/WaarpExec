/**
 * Copyright 2009, Frederic Bregier, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package goldengate.commandexec.utils;

/**
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

    public LocalExecResult(LocalExecResult localExecResult) {
        this.isSuccess = localExecResult.isSuccess;
        this.status = localExecResult.status;
        this.exception = localExecResult.exception;
        this.result = localExecResult.result;
    }
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
