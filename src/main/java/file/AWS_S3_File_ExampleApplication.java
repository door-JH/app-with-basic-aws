package file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class AWS_S3_File_ExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(AWS_S3_File_ExampleApplication.class, args);
	}

}
