package fu.se.badmintonshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class BadmintonshopApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(BadmintonshopApplication.class, args);
	}

	private static void loadDotEnv() {
		for (String pathStr : new String[]{".env", "backend/badmintonshop/.env", "../.env"}) {
			Path path = Path.of(pathStr);
			if (Files.exists(path)) {
				try {
					for (String line : Files.readAllLines(path)) {
						line = line.trim();
						if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
							int eqIdx = line.indexOf('=');
							String key = line.substring(0, eqIdx).trim();
							String val = line.substring(eqIdx + 1).trim();
							if (System.getProperty(key) == null && System.getenv(key) == null) {
								System.setProperty(key, val);
							}
						}
					}
					break;
				} catch (Exception ignored) {
				}
			}
		}
	}

}

