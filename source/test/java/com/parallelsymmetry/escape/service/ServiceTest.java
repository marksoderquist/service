package com.parallelsymmetry.escape.service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import com.parallelsymmetry.escape.utility.DateUtil;
import com.parallelsymmetry.escape.utility.LineParser;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.agent.Agent;
import com.parallelsymmetry.escape.utility.log.DefaultHandler;
import com.parallelsymmetry.escape.utility.log.Log;

public class ServiceTest extends BaseTestCase {

	private static final String MOCK_SERVICE_NAME = "Mock Service";

	private static final String MOCK_RELEASE = "1.0.0 Alpha 00  1973-08-14 22:29:00";

	private MockService service;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		service = new MockService();
		service.call( ServiceFlag.DEVMODE, ServiceFlagValue.TEST, ServiceFlag.SETTINGS_RESET, ServiceFlag.STOP );
		service.getProductManager().setCheckOption( ServiceProductManager.CheckOption.DISABLED );
	}

	public void testBeforeCall() throws Exception {
		assertEquals( "com.parallelsymmetry", service.getCard().getGroup() );
		assertEquals( "mock", service.getCard().getArtifact() );
		assertEquals( "1.0.0-a-00  1973-08-14 22:29:00", service.getCard().getRelease().toString() );
		assertEquals( "(C) 1973-" + DateUtil.getCurrentYear() + " Parallel Symmetry", service.getCard().getCopyright() );
		assertEquals( "All rights reserved.", service.getCard().getCopyrightNotice() );
	}

	public void testCall() throws Exception {
		service.call( ServiceFlag.DEVMODE, ServiceFlagValue.TEST );
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( service.isRunning() );

		assertEquals( MOCK_RELEASE, service.getCard().getRelease().toHumanString() );

		service.call( ServiceFlag.DEVMODE, ServiceFlagValue.TEST, ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( service.isRunning() );
	}

	public void testCommandLineOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( service, Log.INFO, true ) );

		assertCommandLineHeader( parser );

		List<String> lines = parseCommandLineOutput( parser.getRemaining() );

		int indexA = lines.indexOf( "[I] Mock Service started." );
		int indexB = lines.indexOf( "[I] Mock Service stopped." );

		assertTrue( indexA < indexB );
	}

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

	public void testStatusCommandLineOutput() throws Exception {
		LineParser parser = new LineParser( getCommandLineOutput( service, Log.INFO, true, ServiceFlag.STATUS ) );

		assertCommandLineHeader( parser );

		assertEquals( "[I] Mock Service status: STOPPED", parser.next() );
		assertEquals( "", parser.next() );
		assertNull( parser.next() );
	}

	public void testRequiredJavaVersion() throws Exception {
		String version = System.getProperty( "java.runtime.version" );
		System.setProperty( "java.runtime.version", "1.5" );

		LineParser parser = null;

		try {
			parser = new LineParser( getCommandLineOutput( service, Log.INFO, true ) );
		} finally {
			System.setProperty( "java.runtime.version", version );
		}
		assertFalse( "Service should not be running and is.", service.isRunning() );

		assertCommandLineHeader( parser );
		assertEquals( "[E] Java 1.6.0_11 or higher is required, found: 1.5", parser.next() );
	}

	public void testLaunchWithStart() throws Exception {
		//Log.write( "...testLaunchWithStart()..." );

		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 0, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 1, service.getStopCalledCount() );
	}

	public void testLaunchWithStop() throws Exception {
		//Log.write( "...testLaunchWithStop()..." );

		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 0, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 1, service.getStopCalledCount() );
	}

	public void testLaunchWithRestart() throws Exception {
		//Log.write( "...testLaunchWithRestart()..." );

		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 0, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 1, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 0, service.getStopCalledCount() );

		service.call( ServiceFlag.RESTART );
		service.waitForStartup( TIMEOUT, TIMEUNIT );

		Agent.State state = service.getState();
		assertTrue( "Service should be running and is not: " + state, service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 2, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 1, service.getStopCalledCount() );

		service.call( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( "Service should not be running and is.", service.isRunning() );
		assertEquals( "Start method was not called the right amount times.", 2, service.getStartCalledCount() );
		assertEquals( "Stop method was not called the right amount times.", 2, service.getStopCalledCount() );
	}

	public void testFastStartStop() throws Exception {
		//Log.write( "...testFastStartStop()..." );

		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( "Service should not be running and is.", service.isRunning() );

		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( "Service should not be running and is.", service.isRunning() );

		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( "Service should not be running and is.", service.isRunning() );

		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( "Service should not be running and is.", service.isRunning() );
	}

	public void testFastRestarts() throws Exception {
		//Log.write( "...testFastRestarts()..." );

		service.call();
		service.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.restart();
		assertTrue( "Service should be running and is not.", service.isRunning() );
		service.call( ServiceFlag.STOP );
		service.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertFalse( "Service should not be running and is.", service.isRunning() );
	}

	public void testAlreadyRunning() throws Exception {
		//System.out.println( "...testPassParameters()..." );
		String name1 = "Mock Service 1";
		MockService service1 = new MockService( name1 );
		service1.getProductManager().setCheckOption( ServiceProductManager.CheckOption.DISABLED );
		LineParser parser1 = new LineParser( getCommandLineOutput( service1, Log.INFO, false ) );
		System.out.println( parser1.getRemaining() );
		assertCommandLineHeader( name1, parser1 );
		assertTrue( "Service should be running and is not.", service1.isRunning() );

		String name2 = "Mock Service 2";
		MockService service2 = new MockService( name2 );
		service2.getProductManager().setCheckOption( ServiceProductManager.CheckOption.DISABLED );
		LineParser parser2 = new LineParser( getCommandLineOutput( service2, Log.INFO, false ) );

		System.out.println( parser2.getRemaining() );

		service2.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertCommandLineHeader( name2, parser2 );

		assertEquals( "[I] " + name2 + " already running.", parser2.next() );
		assertEquals( "", parser2.next() );
		assertNull( parser2.next() );

		service2.call( ServiceFlag.STOP );
		service1.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertEquals( Service.State.STOPPED, service1.getState() );
		assertFalse( "Service should not be running and is.", service1.isRunning() );

		service2.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertEquals( Service.State.STOPPED, service2.getState() );
		assertFalse( "Service should not be running and is.", service2.isRunning() );
	}

	public void testPassStatus() throws Exception {
		//Log.write( "...testPassStatus()..." );
		String name1 = "Mock Service 1";
		MockService service1 = new MockService( name1 );
		service1.getProductManager().setCheckOption( ServiceProductManager.CheckOption.DISABLED );
		LineParser parser1 = new LineParser( getCommandLineOutput( service1, Log.INFO, false, ServiceFlag.STATUS, "-log.level", "none" ) );
		assertCommandLineHeader( name1, parser1 );

		assertEquals( "[I] " + name1 + " status: STOPPED", parser1.next() );
		assertEquals( "", parser1.next() );
		assertNull( parser1.next() );

		// Start the service.
		assertFalse( "Service should not be running and is.", service1.isRunning() );
		service1.call();
		service1.waitForStartup( TIMEOUT, TIMEUNIT );
		assertTrue( "Service should be running and is not.", service1.isRunning() );

		String name2 = "Mock Service 2";
		MockService service2 = new MockService( name2 );
		service2.getProductManager().setCheckOption( ServiceProductManager.CheckOption.DISABLED );
		LineParser parser2 = new LineParser( getCommandLineOutput( service2, Log.INFO, false, ServiceFlag.STATUS, "-log.level", "none" ) );
		service2.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertCommandLineHeader( name2, parser2 );

		assertEquals( "[I] " + name2 + " already running.", parser2.next() );
		assertEquals( "[I] " + name1 + " status: STARTED", parser2.next() );
		assertEquals( "", parser2.next() );
		assertNull( parser2.next() );

		service2.call( ServiceFlag.STOP );
		service1.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertEquals( Service.State.STOPPED, service1.getState() );
		assertFalse( "Service should not be running and is.", service1.isRunning() );

		service2.waitForShutdown( TIMEOUT, TIMEUNIT );
		assertEquals( Service.State.STOPPED, service2.getState() );
		assertFalse( "Service should not be running and is.", service2.isRunning() );
	}

	private List<String> parseCommandLineOutput( String output ) {
		LineParser parser = new LineParser( output );

		String line = null;
		List<String> lines = new ArrayList<String>();
		while( ( line = parser.next() ) != null ) {
			lines.add( line );
		}

		return lines;
	}

	private String getCommandLineOutput( Service service, Level level, boolean stop, String... commands ) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DefaultHandler handler = new DefaultHandler( new PrintStream( buffer ) );
		handler.setLevel( level );
		Log.addHandler( handler );

		List<String> commandList = new ArrayList<String>( Arrays.asList( commands ) );
		commandList.add( 0, ServiceFlag.DEVMODE );
		commandList.add( 1, ServiceFlagValue.TEST );

		try {
			service.call( commandList.toArray( new String[commandList.size()] ) );
			service.waitForStartup( TIMEOUT, TIMEUNIT );

			if( stop && service.isRunning() ) {
				service.call( new String[] { ServiceFlag.DEVMODE, ServiceFlagValue.TEST, ServiceFlag.STOP } );
				service.waitForShutdown( TIMEOUT, TIMEUNIT );
				assertFalse( service.isRunning() );
			}
		} finally {
			Log.removeHandler( handler );
		}

		return buffer.toString( "UTF-8" );
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
		assertEquals( "", parser.next() );
	}

}
