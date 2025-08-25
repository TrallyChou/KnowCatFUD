package life.trally.knowcatfud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("life.trally.knowcatfud.mapper")
public class KnowCatFudApplication {

	public static void main(String[] args) {
		SpringApplication.run(KnowCatFudApplication.class, args);
	}

}
