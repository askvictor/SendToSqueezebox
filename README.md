# SendToSqueezebox

This is an Android app which makes it easier to send URLs to play on your Squeezebox. At this point it only handles Youtube URLs, but I plan to support arbitrary URLS eventually. For Youtube to work, it requires the Youtube plugininstalled on your Squeezebox server, and is constrained by the same limitations.

Running the app itself only brings up the settings (server address, port). 

To use it, find a video in the Youtube app, the press the Share button, then choose SendToSqueezebox. The app will connect to the chosen server and list the available Squeezebox players if there is more than one (if there is only one player it will send straight to that). 
