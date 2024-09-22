package com.parallelsymmetry.service;

import com.parallelsymmetry.service.product.ProductManager;
import com.parallelsymmetry.utility.DateUtil;
import com.parallelsymmetry.utility.LineParser;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.agent.Agent;
import com.parallelsymmetry.utility.log.DefaultHandler;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.log.LogFlag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceTest extends BaseTestCase {

	private static final String MOCK_SERVICE_NAME = "Mock Service";

	private static final String MOCK_RELEASE = "1.0.0 Alpha 00  1973-08-14 22:29:00";

	private MockService service;

	@BeforeEach
	@Override
	public void setup() throws Exception {
		super.setup();
		service = new MockService();
		service.processInternal( ServiceFlag.EXECMODE, ServiceFlagValue.TEST, ServiceFlag.SETTINGS_RESET, ServiceFlag.STOP );
		service.getProductManager().setCheckOption( ProductManager.CheckOption.MANUAL );
	}

	@AfterEach
	@Override
	public void teardown() throws Exception {
		service.processInternal( ServiceFlag.EXECMODE, ServiceFlagValue.TEST, ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertEquals( Service.State.STOPPED, service.getState() );
		assertFalse( service.isRunning(), "Service should not be running and is." );
		super.teardown();
	}

	@Test
	public void testBeforeCall() throws Exception {
		assertEquals( "com.parallelsymmetry", service.getCard().getGroup() );
		assertEquals( "mock", service.getCard().getArtifact() );
		assertEquals( "1.0.0-a-00  1973-08-14 22:29:00", service.getCard().getRelease().toString() );
		assertEquals( "(C) 1973-" + DateUtil.getCurrentYear() + " Parallel Symmetry", service.getCard().getCopyright() );
		assertEquals( "All rights reserved.", service.getCard().getCopyrightNotice() );
	}

	@Test
	public void testCall() throws Exception {
		service.processInternal( ServiceFlag.EXECMODE, ServiceFlagValue.TEST );
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning() );

		assertEquals( MOCK_RELEASE, service.getCard().getRelease().toHumanString() );
	}

	@Test
	public void testCommandLineOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( service, Log.INFO, true ) );

		assertCommandLineHeader( parser );

		List<String> lines = parseCommandLineOutput( parser.getRemaining() );

		int indexA = lines.indexOf( "[I] Mock Service started." );
		int indexB = lines.indexOf( "[I] Mock Service stopped." );

		assertTrue( indexA < indexB );
	}

	@Test
	public void testHelpCommandLineOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( service, Log.INFO, true, ServiceFlag.HELP ) );

		assertCommandLineHeader( parser );

		assertEquals( "Usage: java -jar <jar file name> [<option>...]", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Commands:", parser.next() );
		assertEquals( "  If no command is specified the program is started.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -help [topic]    Show help information.", parser.next() );
		assertEquals( "  -version         Show version and copyright information only.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "  -stop            Stop the program and exit the VM.", parser.next() );
		assertEquals( "  -status          Print the program status.", parser.next() );
		assertEquals( "  -restart         Restart the program without exiting VM.", parser.next() );
		assertEquals( "  -watch           Watch an already running program.", parser.next() );
		assertEquals( "", parser.next() );
		assertEquals( "Options:", parser.next() );
		assertEquals( "  -log.color           Use ANSI color in the console output.", parser.next() );
		assertEquals( "  -log.level <level>   Change the output log level. Levels are:", parser.next() );
		assertEquals( "                       none, error, warn, info, trace, debug, all", parser.next() );
		assertEquals( "", parser.next() );
		assertNull( parser.next() );
	}

	@Test
	public void testStatusCommandLineOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( service, Log.INFO, true, ServiceFlag.STATUS ) );

		assertCommandLineHeader( parser );

		assertEquals( "[I] Mock Service status: STOPPED", parser.next() );
		assertEquals( "", parser.next() );
		assertNull( parser.next() );
	}

	@Test
	public void testRequiredJavaVersion() throws Exception {
		String version = System.getProperty( "java.runtime.version" );
		System.setProperty( "java.runtime.version", "1.5" );

		LineParser parser = null;

		try {
			parser = new LineParser( getCommandLineOutput( service, Log.INFO, true ) );
		} finally {
			System.setProperty( "java.runtime.version", version );
		}
		assertFalse( service.isRunning(), "Service should not be running and is." );

		assertCommandLineHeader( parser );
		assertEquals( "[E] Java 1.6.0_11 or higher is required, found: 1.5", parser.next() );
	}

	@Test
	public void testLaunchWithStart() throws Exception {
		//Log.write( "...testLaunchWithStart()..." );
		assertFalse( service.isRunning(), "Service should not be running and is." );
		assertEquals( 0, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 0, service.getStopCalledCount(), "Stop method was not called the right amount times." );

		service.processInternal();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning(), "Service should be running and is not." );
		assertEquals( 1, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 0, service.getStopCalledCount(), "Stop method was not called the right amount times." );

		service.processInternal( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning(), "Service should not be running and is." );
		assertEquals( 1, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 1, service.getStopCalledCount(), "Stop method was not called the right amount times." );
	}

	@Test
	public void testLaunchWithStop() throws Exception {
		//Log.write( "...testLaunchWithStop()..." );

		assertFalse( service.isRunning(), "Service should not be running and is." );
		assertEquals( 0, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 0, service.getStopCalledCount(), "Stop method was not called the right amount times." );

		service.processInternal();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning(), "Service should be running and is not." );
		assertEquals( 1, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 0, service.getStopCalledCount(), "Stop method was not called the right amount times." );

		service.processInternal( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning(), "Service should not be running and is." );
		assertEquals( 1, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 1, service.getStopCalledCount(), "Stop method was not called the right amount times." );
	}

	@Test
	public void testLaunchWithRestart() throws Exception {
		//Log.write( "...testLaunchWithRestart()..." );

		assertFalse( service.isRunning(), "Service should not be running and is." );
		assertEquals( 0, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 0, service.getStopCalledCount(), "Stop method was not called the right amount times." );

		service.processInternal();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning(), "Service should be running and is not." );
		assertEquals( 1, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 0, service.getStopCalledCount(), "Stop method was not called the right amount times." );

		service.processInternal( ServiceFlag.RESTART );
		service.waitForStartup( TIMEOUT, TIMEUNIT );

		Agent.State state = service.getState();
		assertTrue( service.isRunning(), "Service should be running and is not: " + state );
		assertEquals( 2, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 1, service.getStopCalledCount(), "Stop method was not called the right amount times." );

		service.processInternal( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning(), "Service should not be running and is." );
		assertEquals( 2, service.getStartCalledCount(), "Start method was not called the right amount times." );
		assertEquals( 2, service.getStopCalledCount(), "Stop method was not called the right amount times." );
	}

	@Test
	public void testFastStartStop() throws Exception {
		//Log.write( "...testFastStartStop()..." );

		service.processInternal();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.processInternal( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning(), "Service should not be running and is." );

		service.processInternal();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.processInternal( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning(), "Service should not be running and is." );

		service.processInternal();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.processInternal( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning(), "Service should not be running and is." );

		service.processInternal();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.processInternal( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning(), "Service should not be running and is." );
	}

	@Test
	public void testFastRestarts() throws Exception {
		//Log.write( "...testFastRestarts()..." );

		service.processInternal();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.restart();
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.restart();
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.restart();
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.restart();
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.restart();
		assertTrue( service.isRunning(), "Service should be running and is not." );
		service.processInternal( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning(), "Service should not be running and is." );
	}

	@Test
	public void testAlreadyRunning() throws Exception {
		//System.out.println( "...testPassParameters()..." );

		// The service name has to be the same for both instances in this test.
		String name = "Mock Service";

		MockService service1 = new MockService( name );
		service1.getProductManager().setCheckOption( ProductManager.CheckOption.MANUAL );
		LineParser parser1 = new LineParser( getCommandLineOutput( service1, Log.INFO, false ) );
		assertCommandLineHeader( name, parser1 );
		assertTrue( service1.isRunning(), "Service should be running and is not." );

		MockService service2 = new MockService( name );
		service2.getProductManager().setCheckOption( ProductManager.CheckOption.MANUAL );
		LineParser parser2 = new LineParser( getCommandLineOutput( service2, Log.INFO, false ) );

		service2.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertCommandLineHeader( name, parser2 );

		assertEquals( "[I] " + name + " connected to peer.", parser2.next() );
		assertEquals( "", parser2.next() );
		assertNull( parser2.next() );

		service2.processInternal( ServiceFlag.STOP );
		service1.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertEquals( Service.State.STOPPED, service1.getState() );
		assertFalse( service1.isRunning(), "Service should not be running and is." );

		service2.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertEquals( Service.State.STOPPED, service2.getState() );
		assertFalse( service2.isRunning(), "Service should not be running and is." );
	}

	@Test
	public void testPassStatus() throws Exception {
		//Log.write( "...testPassStatus()..." );

		// The service name has to be the same for both instances in this test.
		String name = "Mock Service";

		MockService service1 = new MockService( name );
		service1.getProductManager().setCheckOption( ProductManager.CheckOption.MANUAL );
		LineParser parser1 = new LineParser( getCommandLineOutput( service1, Log.INFO, false, ServiceFlag.STATUS, LogFlag.LOG_LEVEL, Log.NONE.toString() ) );
		assertCommandLineHeader( name, parser1 );

		assertEquals( "[I] " + name + " status: STOPPED", parser1.next() );
		assertEquals( "", parser1.next() );
		assertNull( parser1.next() );

		// Start the service.
		assertFalse( service1.isRunning(), "Service should not be running and is." );
		service1.processInternal();
		service1.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service1.isRunning(), "Service should be running and is not." );

		//String name2 = "Mock Service";
		MockService service2 = new MockService( name );
		service2.getProductManager().setCheckOption( ProductManager.CheckOption.MANUAL );
		LineParser parser2 = new LineParser( getCommandLineOutput( service2, Log.INFO, false, ServiceFlag.STATUS, LogFlag.LOG_LEVEL, Log.NONE.toString() ) );
		service2.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertCommandLineHeader( name, parser2 );

		assertEquals( "[I] " + name + " connected to peer.", parser2.next() );
		assertEquals( "[I] " + name + " status: STARTED", parser2.next() );
		assertEquals( "", parser2.next() );
		assertNull( parser2.next() );

		service2.processInternal( ServiceFlag.STOP );
		service1.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertEquals( Service.State.STOPPED, service1.getState() );
		assertFalse( service1.isRunning(), "Service should not be running and is." );

		service2.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertEquals( Service.State.STOPPED, service2.getState() );
		assertFalse( service2.isRunning(), "Service should not be running and is." );
	}

	@Test
	public void testNoupdateFlag() throws Exception {
		service.getProductManager().setCheckOption( ProductManager.CheckOption.MANUAL );
		assertEquals( ProductManager.CheckOption.MANUAL, service.getProductManager().getCheckOption(), "Product manager should be enabled and is not." );

		service.processInternal( ServiceFlag.NOUPDATE );
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning(), "Service should be running and is not." );

		assertEquals( ProductManager.CheckOption.MANUAL, service.getProductManager().getCheckOption(), "Product manager should be disabled and is not." );
	}

	private List<String> parseCommandLineOutput( String output ) {
		LineParser parser = new LineParser( output );

		String line;
		List<String> lines = new ArrayList<>();
		while( (line = parser.next()) != null ) {
			lines.add( line );
		}

		return lines;
	}

	private String getCommandLineOutput( Service service, Level level, boolean stop, String... commands ) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DefaultHandler handler = new DefaultHandler( new PrintStream( buffer ) );
		handler.setLevel( level );
		Log.addHandler( handler );

		List<String> commandList = new ArrayList<>( Arrays.asList( commands ) );
		commandList.add( 0, ServiceFlag.EXECMODE );
		commandList.add( 1, ServiceFlagValue.TEST );

		try {
			service.processInternal( commandList.toArray( new String[ 0 ] ) );
			service.waitForStartup( TIMEOUT, TIMEUNIT );

			if( stop && service.isRunning() ) {
				service.processInternal( ServiceFlag.EXECMODE, ServiceFlagValue.TEST, ServiceFlag.STOP );
				service.waitForShutdown( TIMEOUT, TIMEUNIT );
				assertFalse( service.isRunning() );
			}
		} finally {
			Log.removeHandler( handler );
		}

		return buffer.toString( StandardCharsets.UTF_8 );
	}

	private void assertCommandLineHeader( LineParser parser ) {
		assertCommandLineHeader( MOCK_SERVICE_NAME, parser );
	}

	private void assertCommandLineHeader( String name, LineParser parser ) {
		int currentYear = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).get( Calendar.YEAR );

		String notice = "Mock Service comes with ABSOLUTELY NO WARRANTY. This is open software, and you are welcome to redistribute it under certain conditions.";

		LineParser noticeLines = new LineParser( TextUtil.reline( notice, 75 ) );

		assertEquals( TextUtil.pad( 75, '-' ), parser.next() );
		assertEquals( name + " " + MOCK_RELEASE, parser.next() );
		assertEquals( "(C) 1973-" + currentYear + " Parallel Symmetry All rights reserved.", parser.next() );
		assertEquals( "", parser.next() );
		while( noticeLines.more() ) {
			assertEquals( noticeLines.next(), parser.next() );
		}
		assertEquals( TextUtil.pad( 75, '-' ), parser.next() );
	}

}
