/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.file;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.reporter.api.http.RequestMetrics;
import io.gravitee.reporter.file.config.Config;

@RunWith(MockitoJUnitRunner.class)
public class FileReporterTest {

	@Mock
	Config config;

	@InjectMocks
	FileReporter reporter;
	
	@Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

	@Ignore
	@Test
	public void reportTest() throws Exception {
		File logFile = logFolder.newFile();
		Mockito.when(config.getFilename()).thenReturn(logFile.getAbsolutePath());
		
		LocalDateTime ldt = LocalDateTime.of(2016, Month.FEBRUARY, 29, 16, 23, 6, 99000000);
		LocalDateTime ldt2 = LocalDateTime.of(2015, Month.NOVEMBER, 22, 07, 56, 56, 199000000);
		long[] ts = {ldt.toInstant(ZoneOffset.UTC).toEpochMilli(),
				ldt2.toInstant(ZoneOffset.UTC).toEpochMilli()};
		
		try {
			reporter.start();
			for (int i = 0; i < ts.length; i++) {
				RequestMetrics reportable = RequestMetrics.on(ts[i]).build();
				reportable.setApi("myincredibleapi");
				reportable.setApiKey("kjfhgdjfghdkjhgkdjhgjkdhfghdkghdhdkjfgh");
				reportable.setApiResponseTimeMs(346);
				reportable.setProxyResponseTimeMs(123);
				reportable.setRequestContentLength(12345);
				reportable.setRequestHttpMethod(HttpMethod.POST);
				reportable.setRequestLocalAddress("12.12.12.12");
				reportable.setRequestPath("/dfhgkdlfjgklfgjflkd/yeah");
				reportable.setRequestRemoteAddress("123.123.123.123");
				reportable.setResponseContentLength(12345);
				reportable.setResponseHttpStatus(200);
				
				reporter.report(reportable);
			}
			
			String logContent = new String(Files.readAllBytes(Paths.get(logFile.getAbsolutePath())), StandardCharsets.UTF_8);
			String expected = "[2016-02-29T17:23:06.099+0100] (12.12.12.12) 123.123.123.123 myincredibleapi kjfhgdjfghdkjhgkdjhgjkdhfghdkghdhdkjfgh POST /dfhgkdlfjgklfgjflkd/yeah 200 12345 123"+ System.lineSeparator()+
								"[2015-11-22T08:56:56.199+0100] (12.12.12.12) 123.123.123.123 myincredibleapi kjfhgdjfghdkjhgkdjhgjkdhfghdkghdhdkjfgh POST /dfhgkdlfjgklfgjflkd/yeah 200 12345 123"+ System.lineSeparator();	
			System.out.println(logContent);
			Assert.assertEquals(expected, logContent);
		}
		finally {
			reporter.stop();			
		}
	}
	
}
