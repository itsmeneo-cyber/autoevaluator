package com.autoevaluator;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AutoevaluatorApplication {

	public static void main(String[] args) {
		// Load from .env file for local dev only
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		// Set only if not already defined by Render (which uses env vars)
		setIfAbsent("JDBC_URL", dotenv.get("JDBC_URL", ""));
		setIfAbsent("DB_USER", dotenv.get("DB_USER", ""));
		setIfAbsent("DB_PASS", dotenv.get("DB_PASS", ""));
		setIfAbsent("MAIL_USER", dotenv.get("MAIL_USER", ""));
		setIfAbsent("MAIL_PASS", dotenv.get("MAIL_PASS", ""));
		setIfAbsent("OCR_URL", dotenv.get("OCR_URL", "http://localhost:8000"));

		SpringApplication.run(AutoevaluatorApplication.class, args);
	}

	private static void setIfAbsent(String key, String value) {
		if (System.getenv(key) == null && System.getProperty(key) == null) {
			System.setProperty(key, value);
		}
	}
}
