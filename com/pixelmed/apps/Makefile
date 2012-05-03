PATHTOROOT = ../../..

OBJS = \
SynchronizeFromRemoteSCP.class \
WatchFolderAndSend.class \
RotateFlipSetOfImages.class \
DoseUtility.class \
InsertCodeSequence.class \
MergeCompositeContext.class \
DoseReporterWithLegacyOCRAndAutoSendToRegistry.class \
MultiModalitySimulator.class \
ConvertPaletteColorToGrayscale.class \
ConvertPlanarConfiguration.class \
ConvertNonRGBToRGB.class

all:	${OBJS}

include ${PATHTOROOT}/Makefile.common.mk

.java.class:
	javac ${JAVACOPTIONS} -classpath ${PATHTOROOT}:${DATABASEADDITIONALJARS}:${FTPADDITIONALJARS} -sourcepath ${PATHTOROOT} $<

archive:
	tar -cvf - Makefile *.java | gzip -best > ${PATHTOROOT}/apps.`date '+%Y%m%d'`.tar.gz

testsync:	SynchronizeFromRemoteSCP.class
	mkdir -p /tmp/testimages
	java -cp ${PATHTOROOT}:${DATABASEADDITIONALJARS}:${DICOMADDITIONALJARS} -Djava.awt.headless=true \
		com.pixelmed.apps.SynchronizeFromRemoteSCP \
			/tmp/testdatabase \
			/tmp/testimages \
			localhost 4006 GRAYTOO_DIV_4006 \
			4007 LOCAL4007 1

testrotateflip90H:	RotateFlipSetOfImages.class
	rm -rf /tmp/testrotateflip90H
	mkdir -p /tmp/testrotateflip90H
	java -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.apps.RotateFlipSetOfImages \
			270 Y Y \
			$${HOME}/work/dctool.support/images/dicom/forjpeg2000/discimg \
			/tmp/testrotateflip90H

testwatchandsend:	WatchFolderAndSend.class
	@rm -rf /tmp/watchthisfolder
	@mkdir -p /tmp/watchthisfolder
	../network/runprocessinbackground.sh \
		/tmp/watchandsend.pid /tmp/watchandsend.stdout /tmp/watchandsend.stderr \
		java -cp ${PATHTOROOT} \
			com.pixelmed.apps.WatchFolderAndSend \
			/tmp/watchthisfolder \
			localhost 11112 STORESCP US 1 0
	#tail -f /tmp/watchandsend.stderr &
	@rm -rf /tmp/tesstoragescp.receivedfiles
	@mkdir -p /tmp/tesstoragescp.receivedfiles
	../network/runprocessinbackground.sh \
		/tmp/tesstoragescp.pid /tmp/tesstoragescp.stdout /tmp/tesstoragescp.stderr \
		java -cp ${PATHTOROOT} \
			com.pixelmed.network.StorageSOPClassSCPDispatcher 11112 STORESCP /tmp/tesstoragescp.receivedfiles INSECURE 0
	#tail -f /tmp/tesstoragescp.stderr &
	dcsmpte /tmp/testsmpte.dcm -r PatientName Test^Watch
	#java -cp ${PATHTOROOT} com.pixelmed.network.StorageSOPClassSCU localhost 11112 STORESCP US /tmp/testsmpte.dcm 0 0
	cp /tmp/testsmpte.dcm /tmp/watchthisfolder
	@echo "Sleeping for longer than two watch intervals in order to allow wait for being modified to expire"
	@sleep 25
	@echo "Back from sleep ... folder content should have been processed by now"
	@echo "Dumping logger messages"
	@cat /tmp/watchandsend.stderr
	@echo "List contents of watched directory ... expect it to be empty"
	@ls -lL /tmp/watchthisfolder
	@echo "List done"
	@echo "Comparing sent and received files - expect no difference except meta header"
	# the -a option to grep is necessary since sometimes it thinks the stderr file is binary and fails
	@receivedfilename=`grep -a 'sendReceivedObjectIndication() fileName' /tmp/tesstoragescp.stderr | tail -1 | sed -e 's/^.*fileName: //' -e 's/ from .*$$//'`; \
		andiff /tmp/testsmpte.dcm "$${receivedfilename}"; \
		dccmp /tmp/testsmpte.dcm "$${receivedfilename}"
	@echo "Comparison done"
	@kill -9 `cat /tmp/watchandsend.pid`
	@kill -9 `cat /tmp/tesstoragescp.pid`
	@rm -f /tmp/testsmpte.dcm
	@rm -rf /tmp/watchthisfolder
	@rm -f /tmp/watchandsend.pid /tmp/watchandsend.stdout /tmp/watchandsend.stderr
	@rm -rf /tmp/tesstoragescp.receivedfiles
	@rm -f /tmp/tesstoragescp.pid /tmp/tesstoragescp.stdout /tmp/tesstoragescp.stderr

testdoseutility:	DoseUtility.class
	java -Dcom.sun.management.jmxremote -Xmx2g -XX:+UseCompressedStrings -XX:+UseStringCache -XX:+OptimizeStringConcat -XX:+UseCompressedOops -cp ${PATHTOROOT}:${VIEWERADDITIONALJARS}:${FTPADDITIONALJARS} com.pixelmed.apps.DoseUtility

testinsertcode:	InsertCodeSequence.class
	rm -f /tmp/testinsertcodesrc.dcm
	dcsmpte /tmp/testinsertcodesrc.dcm
	rm -rf /tmp/testinsertcodedst
	mkdir -p /tmp/testinsertcodedst
	java -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.apps.InsertCodeSequence \
			AnatomicRegionSequence "T-A0100" "SRT" "" "Brain" \
			/tmp/testinsertcodesrc.dcm \
			/tmp/testinsertcodedst
	dcdiff /tmp/testinsertcodesrc.dcm /tmp/testinsertcodedst/*
	rm -f /tmp/testinsertcodesrc.dcm
	rm -rf /tmp/testinsertcodedst

testmergecontext:	MergeCompositeContext.class
	rm -rf /tmp/crapd
	mkdir /tmp/crapd
	java -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.apps.MergeCompositeContext \
			/tmp/crapq/QIBA_CT_1B \
			/tmp/crapd
	dcentvfy /tmp/crapd/*.dcm

testmergecontexttwo:	MergeCompositeContext.class
	rm -rf /tmp/crapd
	mkdir /tmp/crapd
	java -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.apps.MergeCompositeContext \
			/tmp/crapq/QIBA_CT_1B/Blinded \
			/tmp/crapq/QIBA_CT_1B/Results \
			/tmp/crapd
	dcentvfy /tmp/crapd/*.dcm

testmergecontextsronly:	MergeCompositeContext.class
	rm -rf /tmp/crapx
	mkdir /tmp/crapx
	(find /tmp/crapq/QIBA_CT_1B/Results -name '*.sr' -exec cp '{}' /tmp/crapx/ ';')
	rm -rf /tmp/crapd
	mkdir /tmp/crapd
	java -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.apps.MergeCompositeContext \
			/tmp/crapx \
			/tmp/crapq/QIBA_CT_1B/Blinded \
			/tmp/crapd
	dcentvfy /tmp/crapd/*.dcm

testmergecontextthree:	MergeCompositeContext.class
	rm -rf /tmp/crapx
	mkdir /tmp/crapx
	dcsmpte /tmp/crapx/initialcontext.dcm \
		-nodisclaimer \
		-r PatientName "MSKCC0001" \
		-r PatientID "MSKCC0001" \
		-r ClinicalTrialSubjectID "MSKCC0001" \
		-r ClinicalTrialSponsorName "QIBA" \
		-r ClinicalTrialProtocolID "QIBA_CT_1B" \
		-r ClinicalTrialProtocolName " " \
		-r ClinicalTrialSiteID "001" \
		-r ClinicalTrialSiteName " " \
		-r ClinicalTrialSubjectID "MSKCC0001" \
		-r ClinicalTrialSubjectReadingID "MSKCC0001" \
		-r StudyInstanceUID "1.3.6.1.4.1.9328.50.1.336855045631083138173485506844321353749"
	rm -rf /tmp/crapd
	mkdir /tmp/crapd
	java -Xmx512m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS} \
		com.pixelmed.apps.MergeCompositeContext \
			/tmp/crapq/QIBA_CT_1B/Blinded \
			/tmp/crapq/QIBA_CT_1B/Results \
			/tmp/crapx \
			/tmp/crapd
	dcentvfy /tmp/crapd/*.dcm

testdosereporter:	DoseReporterWithLegacyOCRAndAutoSendToRegistry.class
	java -server -Djava.awt.headless=true -Xmx256m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS}:${DATABASEADDITIONALJARS}:${FTPADDITIONALJARS} \
		com.pixelmed.apps.DoseReporterWithLegacyOCRAndAutoSendToRegistry \
		testdosereporter.properties

testdosereporterclean:
	rm -rf testdosereporterdb.*
	rm -rf ~/testdosereporterimages

testmodsim:	MultiModalitySimulator.class
	java -server -Djava.awt.headless=true -Xmx384m -cp ${PATHTOROOT}:${DICOMADDITIONALJARS}:${DATABASEADDITIONALJARS} \
		com.pixelmed.apps.MultiModalitySimulator \
			localhost 4011 PACS \
			/Volumes/CluniePortable1TB1_Enc800/database \
			1

testconvertpalette:	ConvertPaletteColorToGrayscale.class
	java -Xmx512m -cp .:${PATHTOROOT} -Djava.awt.headless=true \
		com.pixelmed.apps.ConvertPaletteColorToGrayscale \
		$${HOME}/Pictures/Medical/Color/PaletteColorOfGrayscale/OBJ_0001 \
		/tmp/palette_converted_to_grayscale

testconvertplanar:	ConvertPlanarConfiguration.class
	java -Xmx512m -cp .:${PATHTOROOT} -Djava.awt.headless=true \
		com.pixelmed.apps.ConvertPlanarConfiguration \
		$${HOME}/Pictures/Medical/Color/YBRFULLBYPLANE/ybrfullbyplanecleaned.dcm \
		/tmp/ybrfull_planar_converted_to_pixel.dcm
	java -Xmx512m -cp .:${PATHTOROOT} -Djava.awt.headless=true \
		com.pixelmed.apps.ConvertPlanarConfiguration \
		/tmp/ybrfull_planar_converted_to_pixel.dcm \
		/tmp/ybrfull_planar_converted_back.dcm
	dccmp \
		$${HOME}/Pictures/Medical/Color/YBRFULLBYPLANE/ybrfullbyplanecleaned.dcm \
		/tmp/ybrfull_planar_converted_back.dcm

testconvertybr:	ConvertNonRGBToRGB.class testconvertplanar
	java -Xmx512m -cp .:${PATHTOROOT} -Djava.awt.headless=true \
		com.pixelmed.apps.ConvertNonRGBToRGB \
		$${HOME}/Pictures/Medical/Color/YBRFULLBYPLANE/ybrfullbyplanecleaned.dcm \
		/tmp/ybrfull_planar_converted_to_rgb.dcm
	java -Xmx512m -cp .:${PATHTOROOT} -Djava.awt.headless=true \
		com.pixelmed.apps.ConvertNonRGBToRGB \
		/tmp/ybrfull_planar_converted_to_pixel.dcm \
		/tmp/ybrfull_pixel_converted_to_rgb.dcm
	java -Xmx512m -cp .:${PATHTOROOT} -Djava.awt.headless=true \
		com.pixelmed.apps.ConvertPlanarConfiguration \
		/tmp/ybrfull_pixel_converted_to_rgb.dcm \
		/tmp/ybrfull_pixel_converted_to_rgb_converted_back_to_planar.dcm
	dccmp \
		/tmp/ybrfull_planar_converted_to_rgb.dcm \
		/tmp/ybrfull_pixel_converted_to_rgb_converted_back_to_planar.dcm
