package com.biit.persistence.logger;

import org.apache.log4j.Logger;

import com.biit.logger.BiitLogger;

/**
 * Defines basic log behavior. Uses log4j.properties.
 */
public class ExporterLogger extends BiitLogger {
	
	private static Logger logger = Logger.getLogger(ExporterLogger.class);

	/**
	 * Events that have business meaning (i.e. creating category, deleting form, ...). To follow user actions.
	 */
	public static void info(String className, String message) {
		info(logger,className,message);
	}

	/**
	 * Shows not critical errors. I.e. Email address not found, permissions not allowed for this user, ...
	 * 
	 * @param message
	 */
	public static void warning(String className, String message) {
		warning(logger,className,message);
	}

	/**
	 * For following the trace of the execution. I.e. Knowing if the application access to a method, opening database
	 * connection, etc.
	 */
	public static void debug(String className, String message) {
		debug(logger,className,message);
	}

	/**
	 * To log any not expected error that can cause application malfunction.
	 * 
	 * @param message
	 */
	public static void severe(String className, String message) {
		severe(logger,className,message);
	}

	/**
	 * To log java exceptions and log also the stack trace. If enabled, also can send an email to the administrator to
	 * alert of the error.
	 * 
	 * @param className
	 * @param throwable
	 */
	public static void errorMessage(String className, Throwable throwable) {
		errorMessageNotification(logger,className, getStackTrace(throwable));
	}
}
