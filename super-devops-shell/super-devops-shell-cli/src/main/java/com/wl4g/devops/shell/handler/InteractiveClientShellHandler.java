/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.devops.shell.handler;

import static org.apache.commons.lang3.StringUtils.*;

import java.util.concurrent.atomic.AtomicBoolean;

import com.wl4g.devops.shell.config.Configuration;
import com.wl4g.devops.shell.message.StderrMessage;
import com.wl4g.devops.shell.message.InterruptMessage;
import com.wl4g.devops.shell.message.Message;
import com.wl4g.devops.shell.message.MetaMessage;
import com.wl4g.devops.shell.message.StdoutMessage;
import com.wl4g.devops.shell.message.ConfirmInterruptMessage;
import com.wl4g.devops.shell.message.EOFStdoutMessage;
import com.wl4g.devops.shell.message.AskInterruptMessage;
import com.wl4g.devops.shell.message.BOFStdoutMessage;
import com.wl4g.devops.shell.message.ProgressMessage;

import static com.wl4g.devops.shell.utils.ShellUtils.*;
import static com.wl4g.devops.shell.cli.BuiltInCommand.*;
import static com.wl4g.devops.tool.common.cli.ProcessUtils.*;
import static com.wl4g.devops.shell.config.DefaultShellHandlerRegistrar.getSingle;
import static com.wl4g.devops.shell.message.ChannelState.*;
import static java.lang.String.format;
import static java.lang.System.*;

import org.jline.reader.UserInterruptException;

/**
 * Interactive shell component runner
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年4月14日
 * @since
 */
public class InteractiveClientShellHandler extends AbstractClientShellHandler {

	/** Running status. */
	final private AtomicBoolean running = new AtomicBoolean(false);

	/** Mark the current processing completion status. */
	private volatile boolean lastCompleted = true;

	/** Current command line stdin string. */
	private String stdin;

	/** Payload command last sent timestamp, for timeout check. */
	private long lastCmdSentTime = 0L;

	public InteractiveClientShellHandler(Configuration config) {
		super(config);
	}

	@Override
	public void run(String[] args) {
		if (!running.compareAndSet(false, true)) {
			err.println(format("Already running of '%s'", this));
			return;
		}

		while (true) {
			try {
				printDebug("readLine...");
				stdin = lineReader.readLine(getPrompt());
				printDebug("readLine: " + stdin);
				synchronized (this) { // MARK2
					notifyAll(); // see:MARK3
				}

				// Simulates interrupt events, for secondary
				// development, it is convenient to debug in IDE environment.
				if (DEBUG && "E".equals(trimToEmpty(stdin))) {
					throw new UserInterruptException(stdin);
				}

				// Payload command?
				if (!isBlank(stdin) && isLastComplated()) {
					lastCmdSentTime = currentTimeMillis();
					writeStdin(stdin); // Do send command
					waitForComplete(stdin);
				}
			} catch (UserInterruptException e) { // e.g: Ctrl+C
				// Last command completed, interrupt allowed
				if (isLastComplated()) {
					out.println(format("Command canceled, exit use the command: %s|%s|%s|%s", INTERNAL_EXIT, INTERNAL_EX,
							INTERNAL_QUIT, INTERNAL_QU));
				} else {
					// Last command is not completed, send interrupt signal
					// stop gracefully
					writeStdin(new InterruptMessage(true));
				}
			} catch (Throwable e) {
				printError(EMPTY, e);
			}
		}

	}

	@Override
	protected void postHandleOutput(Object output) throws Exception {
		boolean isWakeup = false;

		if (output instanceof Message) { // Remote command stdout?
			// Meta
			if (output instanceof MetaMessage) {
				MetaMessage meta = (MetaMessage) output;
				getSingle().merge(meta.getRegistedMethods());
				isWakeup = true;
			}
			// Exception
			else if (output instanceof StderrMessage) {
				StderrMessage stderr = (StderrMessage) output;
				printError(EMPTY, stderr.getThrowable());
				isWakeup = true;
			}
			// Progress
			else if (output instanceof ProgressMessage) {
				ProgressMessage pro = (ProgressMessage) output;
				printProgress(pro.getTitle(), pro.getProgress(), pro.getWhole(), '=');
			}
			// Ask interrupt
			else if (output instanceof AskInterruptMessage) {
				AskInterruptMessage ask = (AskInterruptMessage) output;
				printDebug("readLine ask ...");

				// Print ask prompt
				lineReader.printAbove(ask.getSubject());
				synchronized (this) { // MARK3
					wait(TIMEOUT); // see:MARK2
				}
				String confirm = stdin;
				printDebug("readLine ask interrupt confirm: " + confirm);

				// Echo interrupt
				writeStdin(new ConfirmInterruptMessage(isTrue(trimToEmpty(confirm), false)));
			}
			// BOF stdout
			else if (output instanceof BOFStdoutMessage) {
				// Ignore
			}
			// EOF stdout
			else if (output instanceof EOFStdoutMessage) {
				isWakeup = true;
			}
			// Stdout
			else if (output instanceof StdoutMessage) {
				StdoutMessage stdout = (StdoutMessage) output;
				if (stdout.getState() == NEW || stdout.getState() == COMPLETED) {
					// Wakeup lineReader required when output is complete.
					isWakeup = true;
				}
				// Print stdout message.
				out.println(stdout.getContent());
			}
		} else { // Local command stdout?
			isWakeup = true;
		}

		// Direct print of local command stdout.
		if (output instanceof CharSequence) {
			out.println(output);
		}

		// Wakeup for lineReader watching.
		if (isWakeup) {
			wakeup();
		}

	}

	/**
	 * Wait for completed. </br>
	 * {@link AbstractClientShellHandler#wakeup()}
	 * 
	 * @param stdin
	 * @throws InterruptedException
	 */
	private void waitForComplete(String stdin) {
		if (DEBUG) {
			out.println(format("waitForCompleted: %s, completed: %s", this, lastCompleted));
		}
		lastCompleted = false;
	}

	/**
	 * Wake-up for lineReader watching. </br>
	 * 
	 * {@link AbstractClientShellHandler#waitForComplished()}
	 */
	private void wakeup() {
		printDebug(format("Wakeup: %s, completed: %s", this, lastCompleted));
		lastCompleted = true;
	}

	/**
	 * Get the current status prompt.
	 * 
	 * @return
	 */
	private String getPrompt() {
		printDebug(format("getPrompt: %s, completed: %s", this, lastCompleted));
		return lastCompleted ? getAttributed().toAnsi(lineReader.getTerminal()) : EMPTY;
	}

	/**
	 * Gets the current execution return completion status (waiting for
	 * expiration also indicates completion)
	 * 
	 * @return
	 */
	private boolean isLastComplated() {
		return lastCompleted || (currentTimeMillis() - lastCmdSentTime) >= TIMEOUT;
	}

}