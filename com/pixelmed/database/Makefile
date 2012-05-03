PATHTOROOT = ../../..

OBJS = \
DatabaseApplicationProperties.class \
DatabaseInformationModel.class \
DatabaseMediaImporter.class \
DatabaseTreeModel.class \
DatabaseTreeBrowser.class \
DatabaseTreeRecord.class \
DateTimeRangeMatch.class \
DicomDatabaseInformationModel.class \
DicomDatabaseQueryResponseGenerator.class \
DicomDatabaseQueryResponseGeneratorFactory.class \
DicomDatabaseRetrieveResponseGenerator.class \
DicomDatabaseRetrieveResponseGeneratorFactory.class \
DicomDictionaryForMinimalPatientStudySeriesInstanceModel.class \
DicomDictionaryForPatientStudySeriesConcatenationInstanceModel.class \
DicomDictionaryForStudySeriesInstanceModel.class \
MinimalPatientStudySeriesInstanceModel.class \
PatientStudySeriesConcatenationInstanceModel.class \
MapTableBrowser.class \
MapTableModel.class \
RebuildDatabaseFromInstanceFiles.class \
StudySeriesInstanceModel.class \
StudySeriesInstanceSelectiveMatchModel.class

all:	${OBJS}

include ${PATHTOROOT}/Makefile.common.mk

.java.class:
	javac ${JAVACOPTIONS} -classpath ${PATHTOROOT}:${PATHTOADDITIONAL}/hsqldb.jar \
		-sourcepath ${PATHTOROOT} $<

archive:
	tar -cvf - Makefile *.java | gzip -best > ${PATHTOROOT}/database.`date '+%Y%m%d'`.tar.gz

test.empty:
	rm -f test.*
	java -cp ${PATHTOROOT}:${DATABASEADDITIONALJARS}:${DICOMADDITIONALJARS} \
		com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel

test.existing:
	java -cp ${PATHTOROOT}:${DATABASEADDITIONALJARS}:${DICOMADDITIONALJARS} \
		com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel

test.addjpeg2000:
	java -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DATABASEADDITIONALJARS}:${DICOMADDITIONALJARS} \
		com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel \
		${PATHTOROOT}/../../dctool.support/images/dicom/forjpeg2000/discimg/[A-Z][A-Z][0-9]

test.addnemamfmr:
	java -Xms512m -Xmx512m -cp ${PATHTOROOT}:${DATABASEADDITIONALJARS}:${DICOMADDITIONALJARS} \
		com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel \
		${PATHTOROOT}/../../nemamfmr/DISCIMG/IMAGES/*

test.datetime:	DateTimeRangeMatch.class
	java -cp ${PATHTOROOT} com.pixelmed.database.DateTimeRangeMatch

rebuilddatabase:
	rm -f ${PATHTOHOME}/.com.pixelmed.display.DicomImageViewer.database.*
	java -Xmx512m -cp ${PATHTOROOT}:${DATABASEADDITIONALJARS}:${DICOMADDITIONALJARS} \
		com.pixelmed.database.RebuildDatabaseFromInstanceFiles \
		PatientStudySeriesConcatenationInstanceModel \
		${PATHTOHOME}/.com.pixelmed.display.DicomImageViewer.database \
		${PATHTOHOME}/.com.pixelmed.display.DicomImageViewer.images


