package com.saywell.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "gemini.api.key=dummy-test-key")
class BackendApplicationTests {

	@Test
	void contextLoads() {}

}
