PATHTOROOT = ../../..

OBJS = \
ByteArray.class \
CopyStream.class \
FileUtilities.class \
FloatFormatter.class \
HexDump.class \
JTreeWithAdditionalKeyStrokeActions.class \
MessageLogger.class \
PdfDecoder.class \
PdfException.class \
PhoneticStringEncoder.class \
PhoneticStringEncoderException.class \
PrintStreamMessageLogger.class \
ScrollingTextAreaWriter.class \
Syslog.class \
StringUtilities.class \
MACAddress.class \
XPathQuery.class \
UUIDBasedOID.class

all:	${OBJS}

include ${PATHTOROOT}/Makefile.common.mk

archive:
	tar -cvf - Makefile *.java | gzip -best > ../../../utils.`date '+%Y%m%d'`.tar.gz

test:	testStringUtilities

testStringUtilities:	StringUtilities.class
	java -cp ${PATHTOROOT} com.pixelmed.utils.StringUtilities

testByteArray:	ByteArray.class
	java -cp ${PATHTOROOT} com.pixelmed.utils.ByteArray

testSyslog:	Syslog.class
	java -cp ${PATHTOROOT} com.pixelmed.utils.Syslog

testMACAddress:	MACAddress.class
	java -cp ${PATHTOROOT} com.pixelmed.utils.MACAddress

testxpathquery.xml:
	rm -f $@
	echo >>$@ '<?xml version="1.0" encoding="UTF-8"?>'
	echo >>$@ '	<DicomObject>'
	echo >>$@ '		<ImageType element="0008" group="0008" vr="CS">'
	echo >>$@ '			<value number="1">ORIGINAL</value>'
	echo >>$@ '			<value number="2">PRIMARY</value>'
	echo >>$@ '			<value number="3">CINE</value>'
	echo >>$@ '			<value number="4">NONE</value>'
	echo >>$@ '		</ImageType>'
	echo >>$@ '		<ContrastBolusAgentSequence element="0012" group="0018" vr="SQ">'
	echo >>$@ '		  <Item number="1">'
	echo >>$@ '		    <CodeValue element="0100" group="0008" vr="SH">'
	echo >>$@ '		      <value number="1">C-17800</value>'
	echo >>$@ '		    </CodeValue>'
	echo >>$@ '		  </Item>'
	echo >>$@ '		</ContrastBolusAgentSequence>'
	echo >>$@ '</DicomObject>'

testxpathquery:	XPathQuery.class testxpathquery.xml
	java -cp ${PATHTOROOT} com.pixelmed.utils.XPathQuery testxpathquery.xml "/DicomObject/ImageType"
	java -cp ${PATHTOROOT} com.pixelmed.utils.XPathQuery testxpathquery.xml "/DicomObject/ImageType/value"
	java -cp ${PATHTOROOT} com.pixelmed.utils.XPathQuery testxpathquery.xml "/DicomObject/ImageType/value[@number=1]"
	java -cp ${PATHTOROOT} com.pixelmed.utils.XPathQuery testxpathquery.xml "//ImageType/value[@number=1]"
	java -cp ${PATHTOROOT} com.pixelmed.utils.XPathQuery testxpathquery.xml "//ImageType/value[@number=1] = 'ORIGINAL'"
	java -cp ${PATHTOROOT} com.pixelmed.utils.XPathQuery testxpathquery.xml "/DicomObject/ContrastBolusAgentSequence/Item[@number=1]/CodeValue/value[@number=1]"
	java -cp ${PATHTOROOT} com.pixelmed.utils.XPathQuery testxpathquery.xml "//ContrastBolusAgentSequence/Item/CodeValue/value"
	rm -f testxpathquery.xml

testfloatformatter:	FloatFormatter.class
	java -cp ${PATHTOROOT} com.pixelmed.utils.FloatFormatter

