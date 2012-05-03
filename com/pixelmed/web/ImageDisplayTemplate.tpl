<HTML>
<HEAD>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<link rel="stylesheet" type="text/css" href="/stylesheet.css">

<SCRIPT>

//document.onmousemove=ourMouseMove;
//document.onmousedown=ourMouseDown;
//document.onmouseup=ourMouseUp;

var studyUID = "1.2.840.113704.1.111.2512.1069112210.2";
var seriesUID = "1.2.840.113704.1.111.7860.1069113114.20";

var objectUIDs = new Array(

####REPLACEMEWITHLISTOFSOPINSTANCEUIDS####

);

var windowCenters = new Array(

####REPLACEMEWITHWINDOWCENTERS####

);

var windowWidths = new Array(

####REPLACEMEWITHWINDOWWIDTHS####

);

var numberOfImages = objectUIDs.length;

function preLoadImages() {
	var actualImages = new Array(numberOfImages);
	var i;
	for (i=0; i<numberOfImages; ++i) {
		actualImages[i] = new Image;
		actualImages[i].src = makeWadoURL(objectUIDs[i]);
	}
}

function dumpObjectUIDList() {
	document.write("<pre>\n");
	var i;
	for (i=0; i<numberOfImages; ++i) {
		document.write(objectUIDs[i] + "\n");
	}
	document.write("</pre>\n");
}


var currentImageIndex = 0;
var currentImageUrl = null;

var windowWidthDelta = x = 0;
var windowCenterDelta = y = 0;
		
function makeWadoURL(studyUID,seriesUID,objectUID) {
	return "?requestType=WADO&contentType=image/jpeg&studyUID=" + studyUID + "&seriesUID=" + seriesUID + "&objectUID=" + objectUID + "&columns=512" + "&imageQuality=100";
}

function makeWadoURLWithWindow(studyUID,seriesUID,objectUID,windowCenter,windowWidth) {
	return "?requestType=WADO&contentType=image/jpeg&studyUID=" + studyUID + "&seriesUID=" + seriesUID + "&objectUID=" + objectUID + "&windowCenter=" + windowCenter + "&windowWidth=" + windowWidth + "&columns=512" + "&imageQuality=100";
}

function loadCurrentImage() {
	//alert("loadCurrentImage()");
	var newWindowCenter = 0;
	var newWindowWidth = 0;
	if (windowWidths[currentImageIndex] > 0) {
		newWindowCenter = windowCenters[currentImageIndex]+windowCenterDelta;
		newWindowWidth = windowWidths[currentImageIndex]+windowWidthDelta;
		currentImageUrl=makeWadoURLWithWindow(studyUID,seriesUID,objectUIDs[currentImageIndex],newWindowCenter,newWindowWidth);
	}
	else {	// no valid window known
		currentImageUrl=makeWadoURL(studyUID,seriesUID,objectUIDs[currentImageIndex]);
	}
	//self.document.images[0].src=currentImageUrl;
	self.document.getElementById("target").src=currentImageUrl;
	self.document.getElementById("target").onmousedown=ourMouseDown;
	self.document.getElementById("target").onmousemove=null;
	self.document.getElementById("target").onmouseup=null;	

}

function replaceWithNextImage() {
	//alert("replaceWithNextImage()");
	if (++currentImageIndex >= numberOfImages) {
		currentImageIndex=0;
	}
	loadCurrentImage();
}

function loadFirstImage() {
	//alert("loadFirstImage()");
	currentImageIndex=0;
	loadCurrentImage();
}

var mouseMode = 1;	// 1 == scroll, 2 == window

function setMouseModeToScroll() {
	//alert("setMouseModeToScroll()");
	mouseMode = 1;
}

function setMouseModeToWindow() {
	//alert("setMouseModeToWindow()");
	mouseMode = 2;
}


var mouseStartX = -1;
var mouseStartY = -1;
var mouseStartImageIndex = -1;

var scrollDivisor = 5;

function handleDeltaOnMouseMove(x,y) {
	if (mouseMode == 1) {		// scroll
		y = Math.round(y/scrollDivisor);
		newImageIndex = mouseStartImageIndex + y;
		if (newImageIndex < 0) { newImageIndex = 0; }
		else if (newImageIndex >= numberOfImages) { newImageIndex = numberOfImages - 1; }
		if (newImageIndex != currentImageIndex) {
			currentImageIndex = newImageIndex;
			loadCurrentImage();
			self.document.getElementById("target").onmousedown=ourMouseDown;
			self.document.getElementById("target").onmousemove=ourMouseMove;
			self.document.getElementById("target").onmouseup=ourMouseUp;	
		}
	}
}

function handleDeltaOnMouseUp(x,y) {
	if (mouseMode == 2) {		// window
		windowWidthDelta += x;
		windowCenterDelta += y;
		loadCurrentImage();
		self.document.getElementById("target").onmousedown=ourMouseDown;
		self.document.getElementById("target").onmousemove=ourMouseMove;
		self.document.getElementById("target").onmouseup=ourMouseUp;	
	}
}

function ourMouseDown(e) {
	//alert("ourMouseDown()");
	if (!e) { e = ((window.event) ? window.event : ""); }
	if (!e) {
		alert("ourMouseDown(): cannot get event for page position");
	}
	else {
		var posnX;
		var posnY;
		if ((posnX=e.pageX) == null || (posnY=e.pageY) == null) {	// Netscape and Safari
			posnX=event.clientX+document.body.scrollLeft;		// IE
			posnY=event.clientY+document.body.scrollTop;
		}
		mouseStartX = posnX;
		mouseStartY = posnY;
		mouseStartImageIndex = currentImageIndex;
		self.document.getElementById("target").onmousemove=ourMouseMove;
		self.document.getElementById("target").onmouseup=ourMouseUp;	
	}
	return false;		// stops image drag behavior in browser
}

var deltaX = 0;
var deltaY = 0;

function ourMouseUp(e) {
	if (deltaX != 0 || deltaY != 0) {
		//alert("ourMouseMove(): delta x="+deltaX+" y="+deltaY);
		if (mouseMode == 2) {		// window
			handleDeltaOnMouseUp(deltaX, deltaY);
		}
	}
	self.document.getElementById("target").onmousemove=null;
	self.document.getElementById("target").onmouseup=null;	
	mouseStartX = -1;
	mouseStartY = -1;
	mouseStartImageIndex = -1;
	return false;		// stops image drag behavior in browser
}

function ourMouseMove(e) {
	//alert("ourMouseMove()");
	if (!e) { e = ((window.event) ? window.event : ""); }
	if (!e) {
		alert("ourMouseMove(): cannot get event for page position");
	}
	else  if (mouseStartX == -1 || mouseStartY == -1) {
		alert("ourMouseMove(): illegal start x="+mouseStartX+" y="+mouseStartY);
	}
	else {
		var posnX;
		var posnY;
		if ((posnX=e.pageX) == null || (posnY=e.pageY) == null) {	// Netscape and Safari
			posnX=event.clientX+document.body.scrollLeft;		// IE
			posnY=event.clientY+document.body.scrollTop;
		}
		deltaX = posnX-mouseStartX;
		deltaY = posnY-mouseStartY;
		if (deltaX != 0 || deltaY != 0) {
			//alert("ourMouseMove(): delta x="+deltaX+" y="+deltaY);
			if (mouseMode == 1) {		// scroll
				handleDeltaOnMouseMove(deltaX, deltaY);
			}
		}
	}
	return false;		// stops image drag behavior in browser
}

</SCRIPT>
</HEAD>


<BODY>

<SCRIPT>
//alert("starting");
//dumpObjectUIDList();
//document.write("<pre>"+currentImageUrl+"</pre>\n");
//preLoadImages()
</SCRIPT>

<FORM>
<input type="radio" name="mouseMode" value="Scroll" checked onclick="setMouseModeToScroll()"> Scroll
<input type="radio" name="mouseMode" value="Window" onclick="setMouseModeToWindow()"> Window
</FORM>

<img id="target" src="" width="512">

<SCRIPT>loadFirstImage()</SCRIPT>

</BODY>
</HTML>
