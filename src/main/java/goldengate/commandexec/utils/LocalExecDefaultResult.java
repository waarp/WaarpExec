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
 * Default message for LocalExec
 * @author Frederic Bregier
 *
 */
public class LocalExecDefaultResult {
    public static LocalExecResult NoCommand = new LocalExecResult(false, -1, null, "No Command\n");
    public static LocalExecResult BadTransmition = new LocalExecResult(false, -2, null, "Bad Transmission\n");
    public static LocalExecResult NoMessage = new LocalExecResult(false, -3, null, "No Message received\n");
    public static LocalExecResult NotExecutable = new LocalExecResult(false, -4, null, "Not Executable\n");
    public static LocalExecResult BadExecution = new LocalExecResult(false, -5, null, "Bad Execution\n");
    public static LocalExecResult TimeOutExecution = new LocalExecResult(false, -6, null, "TimeOut Execution\n");
    public static LocalExecResult InternalError = new LocalExecResult(false, -7, null, "Internal Error\n");
    public static LocalExecResult NoStatus = new LocalExecResult(false, -8, null, "No Status\n");
    public static LocalExecResult ConnectionRefused = new LocalExecResult(false, -9, null, "Exec Server refused the connection\n");
    public static LocalExecResult CorrectExec = new LocalExecResult(false, 1, null, "Correctly Executed\n");
    public static long RETRYINMS = 500;
    public static long MAXWAITPROCESS = 60000;
    public static String ENDOFCOMMAND = "$#GGEXEC END OF COMMAND#$\n";
}
