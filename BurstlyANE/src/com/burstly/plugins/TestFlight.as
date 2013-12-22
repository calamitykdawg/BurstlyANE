package com.burstly.plugins
{
	
	import flash.external.ExtensionContext;
	
	public class TestFlight
	{
		
		private static var _instance : TestFlight;
		private var _extensionContext : ExtensionContext;
		
		public function TestFlight(enforcer : SingletonEnforcer)
		{
			_extensionContext = ExtensionContext.createExtensionContext("com.burstly.plugins.BurstlyANE", "" );
			if (!_extensionContext) {
				throw new Error( "Burstly ANE is not supported on this platform." );
			}
		}
		
		public static function getInstance() : TestFlight 
		{
			if (!_instance) {
				_instance = new TestFlight(new SingletonEnforcer());
				_instance.init();
			}
			
			return _instance;
		}

		public function dispose() : void
		{ 
			_extensionContext.dispose(); 
		}

		private function init() : void
		{
			_extensionContext.call("init");
		}
		
		/*
			Starts a TestFlight session using the App Token for this app.
		*/
		public function takeOff(appToken : String) : void
		{
			_extensionContext.call("TestFlightWrapper_takeOff", appToken);
		}
		
		/*
			Tracks when a user has passed a checkpoint after the flight has taken off. For example, passed level 1,
			posted a high score. Checkpoints are sent in the background.
		*/
		public function passCheckpoint(checkpoint : String) : void
		{
			_extensionContext.call("TestFlightWrapper_passCheckpoint", checkpoint);
		}
		
	}
}

internal class SingletonEnforcer { }