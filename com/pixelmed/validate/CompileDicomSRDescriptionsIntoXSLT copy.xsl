<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xslout="bla"
	version="1.0">

<xsl:output method="xml" indent="yes"/>

<xsl:namespace-alias stylesheet-prefix="xslout" result-prefix="xsl"/>

<xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz'" />
<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

<xsl:template match="definitions">
	<xslout:stylesheet version="1.0">
	
	<xslout:import href="CommonDicomSRValidationRules.xsl"/>
	
	<xslout:output method="text"/>

	<xslout:template match="/DicomStructuredReport">
	<xslout:choose>
	<xsl:apply-templates select="defineiod"/>
	<xslout:otherwise>
	<xslout:text>IOD (SOP Class) unrecognized</xslout:text><xslout:value-of select="$newline"/>
	</xslout:otherwise>
	</xslout:choose>
	</xslout:template>
	
	<xslout:template match="/DicomStructuredReport/DicomStructuredReportHeader">
		<xslout:apply-templates/>
	</xslout:template>
	
	<xslout:template match="text()"/>

	<xsl:apply-templates select="definecontentitemconstraints"/>

	<xsl:apply-templates select="definetemplate"/>

	<xsl:apply-templates select="usecontextgroups"/>

	</xslout:stylesheet>
</xsl:template>

<xsl:template match="defineiod">
	<xslout:when test="/DicomStructuredReport/DicomStructuredReportContent/container/@sopclass = '{@sopclass}'">
		<xslout:text>Found <xsl:value-of select="@name"/> IOD</xslout:text><xslout:value-of select="$newline"/>
		<xsl:apply-templates select="invokecontentitemconstraints"/>
		<xsl:apply-templates select="invokeroottemplate"/>
		<!-- <xslout:text>IOD validation complete</xslout:text><xslout:value-of select="$newline"/> --> <!-- This is noew written in the invoking source code, after the 2nd pass -->
		<xslout:apply-templates/>
	</xslout:when>
</xsl:template>

<xsl:template match="invokecontentitemconstraints">
	<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking IOD Content Item Constraints: <xsl:value-of select="@name"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
	<xslout:call-template name="{@name}"/>
</xsl:template>

<xsl:template match="definecontentitemconstraints">
	<xslout:template name="{@name}">
	<xsl:apply-templates select="parentcontentitem"/>
	</xslout:template>
</xsl:template>

<xsl:template match="parentcontentitem">
	<xslout:for-each select="//{@name}">
		<xslout:for-each select="*[@relationship]">
			<xslout:choose>
			<xsl:apply-templates select="permittedchildcontentitem"/>
			<xslout:otherwise>
				<xslout:call-template name="describeIllegalChildContentItem"><xslout:with-param name="parent" select=".."/><xslout:with-param name="child" select="."/></xslout:call-template>
			</xslout:otherwise>
			</xslout:choose>
		</xslout:for-each>
	</xslout:for-each>
</xsl:template>

<xsl:template match="permittedchildcontentitem">
	<xslout:when test="@relationship = '{@relationship}' and name(.) = '{@name}'">
		<xslout:call-template name="checkPermittedChildContentItemByValueRelationship"><xslout:with-param name="parent" select=".."/><xslout:with-param name="child" select="."/></xslout:call-template>
	</xslout:when>
	<xslout:when test="@relationship = '{@relationship}' and '{@byreference}' = 'T' and name(.) = 'reference' and name(key('idkey',@IDREF)) = '{@name}'">
		<xslout:call-template name="checkPermittedChildContentItemByValueRelationship"><xslout:with-param name="parent" select=".."/><xslout:with-param name="child" select="."/></xslout:call-template>
	</xslout:when>
</xsl:template>

<xsl:template match="invokeroottemplate">
	<xsl:variable name="tidLabel">TID_<xsl:value-of select="@tid"/></xsl:variable>
	<xsl:choose>
	<xsl:when test="@tid and @templatemappingresource and @cvDocumentTitle and @csdDocumentTitle">
		<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking for presence of Root Template based on presence of Template Identification Sequence and if missing, Document Title: <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xslout:if test="/DicomStructuredReport/DicomStructuredReportContent/container/@template = '{@tid}' and /DicomStructuredReport/DicomStructuredReportContent/container/@templatemappingresource = '{@templatemappingresource}'
		              or /DicomStructuredReport/DicomStructuredReportContent/container/concept/@cv = '{@cvDocumentTitle}' and /DicomStructuredReport/DicomStructuredReportContent/container/concept/@csd = '{@csdDocumentTitle}'">
			<xslout:for-each select="/DicomStructuredReport/DicomStructuredReportContent">
				<xslout:call-template name="{$tidLabel}"/>
			</xslout:for-each>
		</xslout:if>
	</xsl:when>
	<xsl:when test="@cvDocumentTitle and @csdDocumentTitle">
		<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking for presence of Root Template based on Document Title only: <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xslout:if test="/DicomStructuredReport/DicomStructuredReportContent/container/concept/@cv = '{@cvDocumentTitle}' and /DicomStructuredReport/DicomStructuredReportContent/container/concept/@csd = '{@csdDocumentTitle}'">
			<xslout:for-each select="/DicomStructuredReport/DicomStructuredReportContent">
				<xslout:call-template name="{$tidLabel}"/>
			</xslout:for-each>
		</xslout:if>
	</xsl:when>
	<xsl:when test="@tid and @templatemappingresource">
		<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking for presence of Root Template based on presence of Template Identification Sequence only: <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
		<xslout:if test="/DicomStructuredReport/DicomStructuredReportContent/container/@template = '{@tid}' and /DicomStructuredReport/DicomStructuredReportContent/container/@templatemappingresource = '{@templatemappingresource}'">
			<xslout:for-each select="/DicomStructuredReport/DicomStructuredReportContent">
				<xslout:call-template name="{$tidLabel}"/>
			</xslout:for-each>
		</xslout:if>
	</xsl:when>
	</xsl:choose>
</xsl:template>

<xsl:template match="includetemplate">
	<xsl:variable name="tidLabel">TID_<xsl:value-of select="@tid"/></xsl:variable>
	<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Including Template: <xsl:value-of select="$tidLabel"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
	<xsl:choose>
	<xsl:when test="@requiredType ='MC'">
		<xslout:choose>
		<xslout:when test="{@condition}">
			<xslout:call-template name="{$tidLabel}">
				<xslout:with-param name="templatevmmin" select="'{@vmmin}'"/>
				<xslout:with-param name="templatevmmax" select="'{@vmmax}'"/>
				<xslout:with-param name="templateRequiredType" select="'{@requiredType}'"/>
				<xslout:with-param name="templateConditionSatisfied" select="'T'"/>
			</xslout:call-template>
		</xslout:when>
		<xslout:otherwise>
			<xslout:call-template name="{$tidLabel}">
				<xslout:with-param name="templatevmmin" select="'{@vmmin}'"/>
				<xslout:with-param name="templatevmmax" select="'{@vmmax}'"/>
				<xslout:with-param name="templateRequiredType" select="'{@requiredType}'"/>
				<xslout:with-param name="templateConditionSatisfied" select="'F'"/>
			</xslout:call-template>
		</xslout:otherwise>
		</xslout:choose>
	</xsl:when>
	<xsl:otherwise>
		<xslout:call-template name="{$tidLabel}">
				<xslout:with-param name="templatevmmin" select="'{@vmmin}'"/>
				<xslout:with-param name="templatevmmax" select="'{@vmmax}'"/>
			<xslout:with-param name="templateRequiredType" select="'{@requiredType}'"/>
		</xslout:call-template>
	</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="definetemplate">
	<xsl:variable name="tidLabel">TID_<xsl:value-of select="@tid"/></xsl:variable>
	<xslout:template name="{$tidLabel}">
	<xslout:param name="templatevmmin"/>
	<xslout:param name="templatevmmax"/>
	<xslout:param name="templateRequiredType"/>
	<xslout:param name="templateConditionSatisfied"/>
	<xslout:if test="$optionDescribeChecking='T'">
		<xslout:text>Checking Template: <xsl:value-of select="$tidLabel"/> (<xsl:value-of select="@name"/>)</xslout:text><xslout:value-of select="$newline"/>
		<xslout:text>CheckContentItem: templatevmmin = </xslout:text><xslout:value-of select="$templatevmmin"/><xslout:value-of select="$newline"/>
		<xslout:text>CheckContentItem: templatevmmax = </xslout:text><xslout:value-of select="$templatevmmax"/><xslout:value-of select="$newline"/>
		<xslout:text>CheckContentItem: templateRequiredType = </xslout:text><xslout:value-of select="$templateRequiredType"/><xslout:value-of select="$newline"/>
		<xslout:text>CheckContentItem: templateConditionSatisfied = </xslout:text><xslout:value-of select="$templateConditionSatisfied"/><xslout:value-of select="$newline"/>
	</xslout:if>
		<xsl:apply-templates/>
	</xslout:template>
</xsl:template>

<xsl:template match="templatecontentitem">
	<xsl:variable name="description"><xsl:call-template name="buildFullPathInDefinitionToCurrentContentItem"/></xsl:variable>
	<xslout:if test="$optionDescribeChecking='T'"><xslout:text>Checking for content item: <xsl:value-of select="$description"/></xslout:text><xslout:value-of select="$newline"/></xslout:if>
	
	<xsl:variable name="valueType"><xsl:value-of select="translate(@valueType,$uppercase,$lowercase)"/></xsl:variable>       <!-- need lower case ValueType -->
	<xsl:variable name="relationship"><xsl:value-of select="translate(@relationship,$lowercase,$uppercase)"/></xsl:variable> <!-- need upper case relationship -->

	<xsl:choose>
	<xsl:when test="@requiredType ='MC'">
		<xslout:choose>
		<xslout:when test="{@condition}">
			<xslout:call-template name="CheckContentItem">
				<xslout:with-param name="description" select="'{$description}'"/>
				<xslout:with-param name="row" select="'{@row}'"/>
				<xslout:with-param name="relationship" select="'{$relationship}'"/>
				<xslout:with-param name="valueType" select="'{$valueType}'"/>
				<xslout:with-param name="cmConceptName" select="'{@cmConceptName}'"/>
				<xslout:with-param name="csdConceptName" select="'{@csdConceptName}'"/>
				<xslout:with-param name="cvConceptName" select="'{@cvConceptName}'"/>
				<xslout:with-param name="vmmin" select="'{@vmmin}'"/>
				<xslout:with-param name="vmmax" select="'{@vmmax}'"/>
				<xslout:with-param name="requiredType" select="'{@requiredType}'"/>
				<xslout:with-param name="conditionSatisfied" select="'T'"/>
				<xslout:with-param name="mbpo" select="'{@mbpo}'"/>
				<xslout:with-param name="valueSetCID" select="'{@valueSetCID}'"/>
				<xslout:with-param name="valueSetBDE" select="'{@valueSetBDE}'"/>
				<xslout:with-param name="cmValue" select="'{@cmValue}'"/>
				<xslout:with-param name="csdValue" select="'{@csdValue}'"/>
				<xslout:with-param name="cvValue" select="'{@cvValue}'"/>
				<xslout:with-param name="cmUnits" select="'{@cmUnits}'"/>
				<xslout:with-param name="csdUnits" select="'{@csdUnits}'"/>
				<xslout:with-param name="cvUnits" select="'{@cvUnits}'"/>
				<xslout:with-param name="graphicType" select="'{@graphicType}'"/>
				<xslout:with-param name="numpointsmin" select="'{@numpointsmin}'"/>
				<xslout:with-param name="templatevmmin" select="$templatevmmin"/>
				<xslout:with-param name="templatevmmax" select="$templatevmmax"/>
				<xslout:with-param name="templateRequiredType" select="$templateRequiredType"/>
				<xslout:with-param name="templateConditionSatisfied" select="$templateConditionSatisfied"/>
			</xslout:call-template>
		</xslout:when>
		<xslout:otherwise>
			<xslout:call-template name="CheckContentItem">
				<xslout:with-param name="description" select="'{$description}'"/>
				<xslout:with-param name="row" select="'{@row}'"/>
				<xslout:with-param name="relationship" select="'{$relationship}'"/>
				<xslout:with-param name="valueType" select="'{$valueType}'"/>
				<xslout:with-param name="cmConceptName" select="'{@cmConceptName}'"/>
				<xslout:with-param name="csdConceptName" select="'{@csdConceptName}'"/>
				<xslout:with-param name="cvConceptName" select="'{@cvConceptName}'"/>
				<xslout:with-param name="vmmin" select="'{@vmmin}'"/>
				<xslout:with-param name="vmmax" select="'{@vmmax}'"/>
				<xslout:with-param name="requiredType" select="'{@requiredType}'"/>
				<xslout:with-param name="conditionSatisfied" select="'F'"/>
				<xslout:with-param name="mbpo" select="'{@mbpo}'"/>
				<xslout:with-param name="valueSetCID" select="'{@valueSetCID}'"/>
				<xslout:with-param name="valueSetBDE" select="'{@valueSetBDE}'"/>
				<xslout:with-param name="cmValue" select="'{@cmValue}'"/>
				<xslout:with-param name="csdValue" select="'{@csdValue}'"/>
				<xslout:with-param name="cvValue" select="'{@cvValue}'"/>
				<xslout:with-param name="cmUnits" select="'{@cmUnits}'"/>
				<xslout:with-param name="csdUnits" select="'{@csdUnits}'"/>
				<xslout:with-param name="cvUnits" select="'{@cvUnits}'"/>
				<xslout:with-param name="graphicType" select="'{@graphicType}'"/>
				<xslout:with-param name="numpointsmin" select="'{@numpointsmin}'"/>
				<xslout:with-param name="numpointsmax" select="'{@numpointsmax}'"/>
				<xslout:with-param name="templatevmmin" select="$templatevmmin"/>
				<xslout:with-param name="templatevmmax" select="$templatevmmax"/>
				<xslout:with-param name="templateRequiredType" select="$templateRequiredType"/>
				<xslout:with-param name="templateConditionSatisfied" select="$templateConditionSatisfied"/>
			</xslout:call-template>
		</xslout:otherwise>
		</xslout:choose>
	</xsl:when>
	<xsl:otherwise>
			<xslout:call-template name="CheckContentItem">
				<xslout:with-param name="description" select="'{$description}'"/>
				<xslout:with-param name="row" select="'{@row}'"/>
				<xslout:with-param name="relationship" select="'{$relationship}'"/>
				<xslout:with-param name="valueType" select="'{$valueType}'"/>
				<xslout:with-param name="cmConceptName" select="'{@cmConceptName}'"/>
				<xslout:with-param name="csdConceptName" select="'{@csdConceptName}'"/>
				<xslout:with-param name="cvConceptName" select="'{@cvConceptName}'"/>
				<xslout:with-param name="vmmin" select="'{@vmmin}'"/>
				<xslout:with-param name="vmmax" select="'{@vmmax}'"/>
				<xslout:with-param name="requiredType" select="'{@requiredType}'"/>
				<xslout:with-param name="valueSetCID" select="'{@valueSetCID}'"/>
				<xslout:with-param name="valueSetBDE" select="'{@valueSetBDE}'"/>
				<xslout:with-param name="cmValue" select="'{@cmValue}'"/>
				<xslout:with-param name="csdValue" select="'{@csdValue}'"/>
				<xslout:with-param name="cvValue" select="'{@cvValue}'"/>
				<xslout:with-param name="cmUnits" select="'{@cmUnits}'"/>
				<xslout:with-param name="csdUnits" select="'{@csdUnits}'"/>
				<xslout:with-param name="cvUnits" select="'{@cvUnits}'"/>
				<xslout:with-param name="graphicType" select="'{@graphicType}'"/>
				<xslout:with-param name="numpointsmin" select="'{@numpointsmin}'"/>
				<xslout:with-param name="numpointsmax" select="'{@numpointsmax}'"/>
				<xslout:with-param name="templatevmmin" select="$templatevmmin"/>
				<xslout:with-param name="templatevmmax" select="$templatevmmax"/>
				<xslout:with-param name="templateRequiredType" select="$templateRequiredType"/>
				<xslout:with-param name="templateConditionSatisfied" select="$templateConditionSatisfied"/>
			</xslout:call-template>
	</xsl:otherwise>
	</xsl:choose>
	
	<xsl:call-template name="iterateOverChildren">
		<xsl:with-param name="description" select="$description"/>
		<xsl:with-param name="cvConceptName" select="@cvConceptName"/>
		<xsl:with-param name="csdConceptName" select="@csdConceptName"/>
		<xsl:with-param name="conceptNameCID" select="@conceptNameCID"/>
		<xsl:with-param name="valueType" select="$valueType"/>
	</xsl:call-template>

</xsl:template>

<xsl:template name="iterateOverChildren">
	<xsl:param name="description"/>
	<xsl:param name="cvConceptName"/>
	<xsl:param name="csdConceptName"/>
	<xsl:param name="conceptNameCID"/>
	<xsl:param name="valueType"/>
	<xsl:choose>
	<xsl:when test="string-length($cvConceptName) &gt; 0">
		<xslout:for-each select="child::node()[name() = '{$valueType}' and concept/@cv = '{$cvConceptName}' and concept/@csd = '{$csdConceptName}']">
			<xsl:apply-templates/>
		</xslout:for-each>
	</xsl:when>
	<xsl:when test="string-length($conceptNameCID) &gt; 0">
		<xsl:variable name="currentNode" select="."/>
		<xsl:variable name="contextGroupWanted" select="document('DicomContextGroupsSource.xml')/definecontextgroups/definecontextgroup[@cid=$conceptNameCID]"/>
		<xsl:variable name="contextGroupCodes" select="$contextGroupWanted/contextgroupcode"/>
		<xsl:choose>
		<xsl:when test="count($contextGroupCodes) &gt; 0">
			<xsl:for-each select="$contextGroupCodes">
				<xslout:for-each select="child::node()[name() = '{$valueType}' and concept/@cv = '{@cv}' and concept/@csd = '{@csd}']">
					<xsl:apply-templates select="$currentNode/*"/>
				</xslout:for-each>
			</xsl:for-each>
		</xsl:when>
		<xsl:otherwise>
			<xslout:text>Internal Error: </xslout:text><xsl:value-of select="$description"/><xslout:text>: Concept Name CID is empty or missing - </xslout:text><xsl:value-of select="$currentNode/@conceptNameCID"/><xslout:value-of select="$newline"/>
		</xsl:otherwise>
		</xsl:choose>
	</xsl:when>
	<xsl:otherwise>
		<xslout:for-each select="child::node()[name() = '{$valueType}']">
			<xsl:apply-templates/>
		</xslout:for-each>
	</xsl:otherwise>
	</xsl:choose>
</xsl:template>


<xsl:template match="verify">
	<xsl:variable name="description">
		<xsl:call-template name="buildFullPathInDefinitionToCurrentContentItem"/>
	</xsl:variable>
	<xslout:if test="{@test}">
		<xslout:text><xsl:value-of select="@status"/>: <xsl:value-of select="$description"/>: </xslout:text>
		<xslout:call-template name="buildFullPathInInstanceToCurrentNode"/>
		<xslout:text>: <xsl:value-of select="@message"/></xslout:text><xslout:value-of select="$newline"/>
	</xslout:if>
</xsl:template>

<xsl:template name="buildFullPathInDefinitionToCurrentContentItem">
	<xsl:if test="name(.) != 'definitions'">
		<xsl:for-each select="..">
			<xsl:call-template name="buildFullPathInDefinitionToCurrentContentItem"/>
		</xsl:for-each>
		<xsl:choose>
		<xsl:when test="string-length(@row) != 0">
			<xsl:text>/</xsl:text>
			<xsl:text>[Row </xsl:text>
			<xsl:value-of select="@row"/>
			<xsl:text>] </xsl:text>
			<xsl:value-of select="translate(@valueType,$lowercase,$uppercase)"/>
			<xsl:text> (</xsl:text>
			<xsl:value-of select="@cvConceptName"/>
			<xsl:text>,</xsl:text>
			<xsl:value-of select="@csdConceptName"/>
			<xsl:text>,"</xsl:text>
			<xsl:value-of select="@cmConceptName"/>
			<xsl:text>")</xsl:text>
		</xsl:when>
		<xsl:when test="string-length(@name) != 0">
			<xsl:if test="string-length(@tid) != 0">
				<xsl:text>Template </xsl:text>
				<xsl:value-of select="@tid"/>
				<xsl:text> </xsl:text>
			</xsl:if>
			<xsl:value-of select="@name"/>
		</xsl:when>
		</xsl:choose>
	</xsl:if>
</xsl:template>

<xsl:template name="buildFullPathInDefinitionToCurrentNode">
	<xsl:if test="name(.) != 'definitions'">
		<xsl:for-each select="..">
			<xsl:call-template name="buildFullPathInDefinitionToCurrentNode"/>
		</xsl:for-each>
		<xsl:value-of select="@name"/>
		<xsl:text>/</xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template match="usecontextgroups">
	<xsl:apply-templates select="usecontextgroup"/>
</xsl:template>

<xsl:template match="usecontextgroup">
	<xsl:variable name="cidLabel">CID_<xsl:value-of select="@cid"/></xsl:variable>
	<xslout:key name="{$cidLabel}" match="definecontextgroup[@cid = '{@cid}']/contextgroupcode" use="@cv"/>
</xsl:template>

</xsl:stylesheet>
