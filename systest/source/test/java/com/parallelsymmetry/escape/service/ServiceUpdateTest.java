package com.parallelsymmetry.escape.service;

import java.io.File;
import java.util.List;

import com.parallelsymmetry.escape.product.ProductManager;
import com.parallelsymmetry.escape.utility.FileUtil;
import com.parallelsymmetry.escape.utility.TextUtil;
import com.parallelsymmetry.escape.utility.log.Log;
import com.parallelsymmetry.escape.utility.setting.Settings;
import com.parallelsymmetry.service.systest.VerifyService;

public class ServiceUpdateTest extends BaseTestCase {

	private VerifyService service;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Log.setLevel( Log.NONE );
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
		service.call( ServiceFlag.EXECMODE, ServiceFlagValue.TEST, ServiceFlag.SETTINGS_RESET, ServiceFlag.STOP );

		// Configure the service update settings.
		Settings updateSettings = service.getSettings().getNode( Service.PRODUCT_MANAGER_SETTINGS_PATH );
		updateSettings.put( "check", "STARTUP" );
		updateSettings.flush();

		// Start the service that detects the update, stages the update, and restarts.
		Log.write( Log.ERROR, "Executing update..." );
		executeUpdate();

		// Because the stage process should restart the application immediately 
		// following an update the test needs to wait for all the processes to 
		// terminate and for the auto terminate timeout also.
		Thread.sleep( PROCESS_EXECUTE_TIME + VerifyService.AUTO_TERMINATE_TIMEOUT );

		Log.write( Log.ERROR, "Verifying results..." );

		String applyLog = FileUtil.load( new File( INSTALL, "verify.log" ) );
		List<String> applyLogLines = TextUtil.getLines( applyLog );

		// Check the service log for correct entries.
		int serviceIndex = 0;
		assertTrue( "Updates not detected", ( serviceIndex = findLine( applyLogLines, UPDATES_DETECTED, serviceIndex ) ) >= 0 );
		assertTrue( "Program exit not detected", ( serviceIndex = findLine( applyLogLines, PROGRAM_EXITING, serviceIndex ) ) >= 0 );

		// Check the updater log for correct entries.
		File update = new File( service.getProgramDataFolder(), ProductManager.UPDATE_FOLDER_NAME + "/" + service.getProductManager().getStagedUpdateFileName( service.getCard() ) );

		Log.write( Log.TRACE, "Looking for: " + "[I] Successful update: " + update.getCanonicalPath() );
		assertEquals( "Update apply not detected", 1, countLines( applyLogLines, "[I] Successful update: " + update.getCanonicalPath() ) );

		// Check the verify service log for correct entries.
		assertTrue( "Service start not detected", ( serviceIndex = findLine( applyLogLines, SERVICE_STARTED, serviceIndex ) ) >= 0 );
		assertTrue( "Service stop not detected", ( serviceIndex = findLine( applyLogLines, SERVICE_STOPPED, serviceIndex ) ) >= 0 );

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
