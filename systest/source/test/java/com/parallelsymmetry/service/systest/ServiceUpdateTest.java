package com.parallelsymmetry.service.systest;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import com.parallelsymmetry.service.ServiceFlag;
import com.parallelsymmetry.service.ServiceFlagValue;
import com.parallelsymmetry.service.ServiceSettingsPath;
import com.parallelsymmetry.service.product.ProductManager;
import com.parallelsymmetry.utility.FileUtil;
import com.parallelsymmetry.utility.TextUtil;
import com.parallelsymmetry.utility.log.Log;
import com.parallelsymmetry.utility.log.LogFlag;
import com.parallelsymmetry.utility.setting.Settings;

public class ServiceUpdateTest extends BaseTestCase {

	private VerifyService service;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Log.setLevel( Log.TRACE );
	}

	/*
	 * This is a complex test that verifies that updates will work. The test needs
	 * to set up the service to check for updates, execute the service once (not
	 * applying updates) to stage the updates, then executing the service again to
	 * apply the updates, which in turn requires the service to be restarted and
	 * the release number verified.
	 */
	public void testUpdate() throws Exception {
		clean();
		stage();

		// Create an instance of the service for configuration purposes.
		Log.write( Log.ERROR, "Configuring service..." );
		service = new VerifyService();
		assertNotNull( service.getCard() );

		// Reset the preferences but don't start the service.
		Log.write( "Resetting service settings..." );
		service.processInternal( ServiceFlag.EXECMODE, ServiceFlagValue.TEST, ServiceFlag.SETTINGS_RESET, LogFlag.LOG_DATE, ServiceFlag.STOP );
		Log.setShowDate( false );

		// Configure file locations.
		File verifyLogFile = new File( INSTALL, "verify.log" );
		File updateLogFile = new File( service.getProductDataFolder(), "updater.log" );

		// Remove old log file.
		assertTrue( FileUtil.delete( updateLogFile ) );

		// Configure the service update settings.
		Settings updateSettings = service.getSettings().getNode( ServiceSettingsPath.PRODUCT_MANAGER_SETTINGS_PATH );
		updateSettings.put( "check", "STARTUP" );
		updateSettings.flush();

		// Start the service that detects the update, stages the update, and restarts.
		Log.write( Log.ERROR, "Executing update and waiting for results..." );
		executeUpdate();

		// Because the stage process should restart the application immediately 
		// following an update the test needs to wait for all the processes to 
		// terminate and for the auto terminate timeout also.
		Thread.sleep( PROCESS_EXECUTE_TIME + VerifyService.AUTO_TERMINATE_TIMEOUT );

		Log.write( Log.ERROR, "Verifying update results..." );

		// Load the verify service log file.
		assertTrue( "Verify log file not found: " + verifyLogFile, verifyLogFile.exists() );
		String applyLog = FileUtil.load( verifyLogFile );
		List<String> applyLogLines = TextUtil.getLines( applyLog );

		// Load the updater log file.
		assertTrue( "Updater log file not found: " + updateLogFile, updateLogFile.exists() );
		String updateLog = FileUtil.load( updateLogFile );
		List<String> updateLogLines = TextUtil.getLines( updateLog );

		try {
			// Check the service log for correct entries.
			int serviceIndex = 0;
			assertTrue( "Updates not detected", ( serviceIndex = TextUtil.findLineIndex( applyLogLines, UPDATES_DETECTED, serviceIndex ) ) >= 0 );
			assertTrue( "Program exit not detected", ( serviceIndex = TextUtil.findLineIndex( applyLogLines, PROGRAM_EXITING, serviceIndex ) ) >= 0 );

			// Check the updater log for correct entries.
			File update = new File( service.getProductDataFolder(), ProductManager.UPDATE_FOLDER_NAME + "/" + service.getProductManager().getStagedUpdateFileName( service.getCard() ) );

			Log.write( Log.TRACE, "Looking for: " + "[I] <timestamp> Successful update: " + update.getCanonicalPath() );
			try {
				assertEquals( "Update success not detected", 1, TextUtil.countLines( updateLogLines, "\\[I\\] " + TIMESTAMP + " Successful update: " + Pattern.quote( update.getCanonicalPath() ) ) );
			} catch( AssertionError error ) {
				Log.write( Log.ERROR, updateLog );
				throw error;
			}

			// Check the verify service log for correct entries.
			assertTrue( "Service start not detected", ( serviceIndex = TextUtil.findLineIndex( applyLogLines, SERVICE_STARTED, serviceIndex ) ) >= 0 );
			assertTrue( "Service stop not detected", ( serviceIndex = TextUtil.findLineIndex( applyLogLines, SERVICE_STOPPED, serviceIndex ) ) >= 0 );
		} catch( AssertionError error ) {
			System.out.println( applyLog );
			throw error;
		}

		// Verify the file codes.
		for( String library : libraries ) {
			assertEquals( "Verification code", codes.get( library ), getVerifyCode( new File( INSTALL, library ) ) );
		}

		/*
		 * Due to a known bug, it is impossible to consistently validate that the
		 * updates have all been processed from this VM using the preferences API:
		 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6714335
		 */
	}
}
