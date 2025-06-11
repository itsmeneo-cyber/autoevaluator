package com.autoevaluator;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AutoevaluatorApplication {

	public static void main(String[] args) {
		// Load environment variables from .env file (in project root)
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing() // avoid crash on Render
				.load();

		// Set required properties so Spring Boot can resolve ${...}
		System.setProperty("JDBC_URL", dotenv.get("JDBC_URL", ""));
		System.setProperty("DB_USER", dotenv.get("DB_USER", ""));
		System.setProperty("DB_PASS", dotenv.get("DB_PASS", ""));
		System.setProperty("MAIL_USER", dotenv.get("MAIL_USER", ""));
		System.setProperty("MAIL_PASS", dotenv.get("MAIL_PASS", ""));

		// ✅ Add OCR URL support
		System.setProperty("OCR_URL", dotenv.get("OCR_URL", "http://localhost:8000"));

		SpringApplication.run(AutoevaluatorApplication.class, args);
	}
}
