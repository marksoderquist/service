module com.parallelsymmetry.service {
	// Compile time only

	// Compile and runtime
	requires com.parallelsymmetry.updater;
	requires com.parallelsymmetry.utility;
	requires java.management;
	requires java.logging;
	requires java.desktop;

	exports com.parallelsymmetry.service;
}
