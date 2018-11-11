package org.idempiere.common.util

import org.slf4j.LoggerFactory
import java.awt.Container
import java.awt.Graphics
import java.awt.Window
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Properties
import javax.swing.JDialog
import javax.swing.JFrame

/**
 * System Environment and static variables.
 *
 * @author Jorg Janke
 * @version $Id: Env.java,v 1.3 2006/07/30 00:54:36 jjanke Exp $
 * @author Teo Sarca, www.arhipac.ro
 *  * BF [ 1619390 ] Use default desktop browser as external browser
 *  * BF [ 2017987 ] Env.getContext(TAB_INFO) should NOT use global context
 *  * FR [ 2392044 ] Introduce Env.WINDOW_MAIN
 */
 object Env {
 val STANDARD_REPORT_FOOTER_TRADEMARK_TEXT = "#STANDARD_REPORT_FOOTER_TRADEMARK_TEXT"

 val AD_ROLE_ID = "#AD_Role_ID"

 val AD_USER_ID = "#AD_User_ID"

 val AD_ORG_ID = "#AD_Org_ID"

 val AD_CLIENT_ID = "#AD_Client_ID"

 val AD_ORG_NAME = "#AD_Org_Name"

 val M_WAREHOUSE_ID = "#M_Warehouse_ID"

private val clientContextProvider = DefaultContextProvider()

private val eventListeners = ArrayList<IEnvEventListener>()

 var adWindowDummyID = 200054

/** Logger  */
  private val log = LoggerFactory.getLogger(javaClass)

/**
 * ************************************************************************ Application Context
 */
  /** WindowNo for Main  */
   val WINDOW_MAIN = 0

/** Tab for Info  */
   val TAB_INFO = 1113

/**
 * Get Context
 *
 * @return Properties
 */
  /**
 * Replace the contents of the current session/process context. Don't use this to setup a new
 * session/process context, use ServerContext.setCurrentInstance instead.
 *
 * @param ctx context
 */
  // nothing to do if ctx is already the current context
 var ctx: Properties?
get() = contextProvider.context
set(ctx) {
    if (ctx == null) throw IllegalArgumentException("Require Context")
    if (ServerContext.currentInstance === ctx) return

    ctx.clear()
    ctx.putAll(ctx)
} //  getCtx
 //  setCtx

/**
 * @param provider
 */
   var contextProvider: ContextProvider
get() = ServerContextProvider.INSTANCE
@Deprecated("")
set(provider) {}

 val DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"

/**
 * JDBC Timestamp Format yyyy-mm-dd hh:mm:ss
 *
 * @return timestamp format
 */
   val timestampFormat_Default: SimpleDateFormat
get() = SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT) //  getTimestampFormat_JDBC

/** ************************************************************************ Language issues  */

  /** Context Language identifier  */
   val LANGUAGE = "#AD_Language"

/** Context for POS ID  */
   val POS_ID = "#POS_ID"

/** ************************************************************************ Static Variables  */

  /** Big Decimal 0  */
   val ZERO = BigDecimal.valueOf(0.0)
/** Big Decimal 1  */
   val ONE = BigDecimal.valueOf(1.0)
/** Big Decimal 100  */
   val ONEHUNDRED = BigDecimal.valueOf(100.0)

/** New Line  */
   val NL = System.getProperty("line.separator")

/** @param listener
 */
   fun addEventListener(listener: IEnvEventListener) {
eventListeners.add(listener)
}

/**
 * @param listener
 * @return boolean
 */
   fun removeEventListener(listener: IEnvEventListener): Boolean {
return eventListeners.remove(listener)
}

/**
 * Set Global Context to Value
 *
 * @param ctx context
 * @param context context key
 * @param value context value
 */
   fun setContext(ctx: Properties?, context: String?, value: String?) {
if (ctx == null || context == null) return
if (log.isTraceEnabled) log.trace("Context $context==$value")
 //
    if (value == null || value!!.length == 0)
ctx!!.remove(context)
else
ctx!!.setProperty(context, value)
} // 	setContext

/**
 * Set Global Context to Value
 *
 * @param ctx context
 * @param context context key
 * @param value context value
 */
   fun setContext(ctx: Properties?, context: String?, value: Timestamp?) {
if (ctx == null || context == null) return
if (value == null) {
ctx!!.remove(context)
if (log.isTraceEnabled) log.trace("Context $context==$value")
} else { // 	JDBC Format	2005-05-09 00:00:00.0
 // BUG:3075946 KTU, Fix Thai Date
      // String stringValue = value.toString();
      var stringValue = ""
val c1 = Calendar.getInstance()
    c1.time = value!!
stringValue = timestampFormat_Default.format(c1.time)
 // 	Chop off .0 (nanos)
      // stringValue = stringValue.substring(0, stringValue.indexOf("."));
      // KTU
      ctx!!.setProperty(context, stringValue)
if (log.isTraceEnabled) log.trace("Context $context==$stringValue")
}
} // 	setContext

/**
 * Set Global Context to (int) Value
 *
 * @param ctx context
 * @param context context key
 * @param value context value
 */
   fun setContext(ctx: Properties?, context: String?, value: Int) {
if (ctx == null || context == null) return
if (log.isTraceEnabled) log.trace("Context $context==$value")
 //
    ctx!!.setProperty(context, value.toString())
} // 	setContext

/**
 * Set Global Context to Y/N Value
 *
 * @param ctx context
 * @param context context key
 * @param value context value
 */
   fun setContext(ctx: Properties, context: String, value: Boolean) {
setContext(ctx, context, convert(value))
} // 	setContext

/**
 * Set Context for Window to Value
 *
 * @param ctx context
 * @param WindowNo window no
 * @param context context key
 * @param value context value
 */
   fun setContext(ctx: Properties?, WindowNo: Int, context: String?, value: String?) {
if (ctx == null || context == null) return
if (log.isTraceEnabled)
log.trace("Context($WindowNo) $context==$value")
 //
    if (value == null || value == "")
ctx!!.remove((WindowNo).toString() + "|" + context)
else
ctx!!.setProperty((WindowNo).toString() + "|" + context, value)
} // 	setContext

/**
 * Set Context for Window to Value
 *
 * @param ctx context
 * @param WindowNo window no
 * @param context context key
 * @param value context value
 */
   fun setContext(ctx: Properties?, WindowNo: Int, context: String?, value: Timestamp?) {
if (ctx == null || context == null) return
if (value == null) {
ctx!!.remove((WindowNo).toString() + "|" + context)
if (log.isTraceEnabled)
log.trace("Context($WindowNo) $context==$value")
} else { // 	JDBC Format	2005-05-09 00:00:00.0
 // BUG:3075946 KTU, Fix Thai year
      // String stringValue = value.toString();
      var stringValue = ""
val c1 = Calendar.getInstance()
    c1.time = value!!
stringValue = timestampFormat_Default.format(c1.time)
 // 	Chop off .0 (nanos)
      // stringValue = stringValue.substring(0, stringValue.indexOf("."));
      // KTU
      ctx!!.setProperty((WindowNo).toString() + "|" + context, stringValue)
if (log.isTraceEnabled)
log.trace("Context($WindowNo) $context==$stringValue")
}
} // 	setContext

/**
 * Set Context for Window to int Value
 *
 * @param ctx context
 * @param WindowNo window no
 * @param context context key
 * @param value context value
 */
   fun setContext(ctx: Properties?, WindowNo: Int, context: String?, value: Int) {
if (ctx == null || context == null) return
if (log.isTraceEnabled)
log.trace("Context($WindowNo) $context==$value")
 //
    ctx!!.setProperty((WindowNo).toString() + "|" + context, value.toString())
} // 	setContext

 fun setContext(
     ctx: Properties?,
     WindowNo: Int,
     TabNo: Int,
     context: String?,
     value: Int
 ) {
if (ctx == null || context == null) return
if (log.isTraceEnabled)
log.trace("Context($WindowNo) $context==$value")
 //
    ctx!!.setProperty((WindowNo).toString() + "|" + TabNo + "|" + context, value.toString())
} // 	setContext

/**
 * Set Context for Window to Y/N Value
 *
 * @param ctx context
 * @param WindowNo window no
 * @param context context key
 * @param value context value
 */
   fun setContext(ctx: Properties, WindowNo: Int, context: String, value: Boolean) {
setContext(ctx, WindowNo, context, convert(value))
} // 	setContext

private fun convert(value: Boolean): String {
return if (value) "Y" else "N"
}

/**
 * Set Context for Window to Y/N Value
 *
 * @param ctx context
 * @param WindowNo window no
 * @param context context key
 * @param value context value
 */
   fun setContext(
       ctx: Properties,
       WindowNo: Int,
       TabNo: Int,
       context: String,
       value: Boolean
   ) {
setContext(ctx, WindowNo, TabNo, context, convert(value))
} // 	setContext

/**
 * Set Context for Window & Tab to Value
 *
 * @param ctx context
 * @param WindowNo window no
 * @param TabNo tab no
 * @param context context key
 * @param value context value
 */
   fun setContext(
       ctx: Properties?,
       WindowNo: Int,
       TabNo: Int,
       context: String?,
       value: String?
   ) {
var value = value
if (ctx == null || context == null) return
if (log.isTraceEnabled)
log.trace("Context($WindowNo,$TabNo) $context==$value")
 //
    if (value == null)
if (context!!.endsWith("_ID"))
 // TODO: Research potential problems with tables with Record_ID=0
        value = "0"
else
value = ""
ctx!!.setProperty((WindowNo).toString() + "|" + TabNo + "|" + context, value)
} // 	setContext

/**
 * Set Auto Commit
 *
 * @param ctx context
 * @param autoCommit auto commit (save) @Deprecated user setProperty instead
 */
  @Deprecated("")
 fun setAutoCommit(ctx: Properties?, autoCommit: Boolean) {
if (ctx == null) return
ctx!!.setProperty("AutoCommit", convert(autoCommit))
} // 	setAutoCommit

/**
 * Set Auto Commit for Window
 *
 * @param ctx context
 * @param WindowNo window no
 * @param autoCommit auto commit (save)
 */
   fun setAutoCommit(ctx: Properties?, WindowNo: Int, autoCommit: Boolean) {
if (ctx == null) return
ctx!!.setProperty((WindowNo).toString() + "|AutoCommit", convert(autoCommit))
} // 	setAutoCommit

/**
 * Set Auto New Record
 *
 * @param ctx context
 * @param autoNew auto new record @Deprecated user setProperty instead
 */
  @Deprecated("")
 fun setAutoNew(ctx: Properties?, autoNew: Boolean) {
if (ctx == null) return
ctx!!.setProperty("AutoNew", convert(autoNew))
} // 	setAutoNew

/**
 * Set Auto New Record for Window
 *
 * @param ctx context
 * @param WindowNo window no
 * @param autoNew auto new record
 */
   fun setAutoNew(ctx: Properties?, WindowNo: Int, autoNew: Boolean) {
if (ctx == null) return
ctx!!.setProperty((WindowNo).toString() + "|AutoNew", convert(autoNew))
} // 	setAutoNew

/**
 * Set SO Trx
 *
 * @param ctx context
 * @param isSOTrx SO Context
 */
   fun setSOTrx(ctx: Properties?, isSOTrx: Boolean) {
if (ctx == null) return
ctx!!.setProperty("IsSOTrx", convert(isSOTrx))
} // 	setSOTrx

/**
 * Get global Value of Context
 *
 * @param ctx context
 * @param context context key
 * @return value or ""
 */
   fun getContext(ctx: Properties?, context: String?): String? {
if (ctx == null || context == null) throw IllegalArgumentException("Require Context")
return ctx!!.getProperty(context, "")
} // 	getContext

/**
 * Get Value of Context for Window. if not found global context if available and enabled
 *
 * @param ctx context
 * @param WindowNo window
 * @param context context key
 * @param onlyWindow if true, no defaults are used unless explicitly asked for
 * @return value or ""
 */
  @JvmOverloads fun getContext(
      ctx: Properties?,
      WindowNo: Int,
      context: String?,
      onlyWindow: Boolean = false
  ): String? {
if (ctx == null) throw IllegalArgumentException("No Ctx")
if (context == null) throw IllegalArgumentException("Require Context")
val s = ctx!!.getProperty((WindowNo).toString() + "|" + context)
if (s == null) {
 // 	Explicit Base Values
      if (context!!.startsWith("#") || context!!.startsWith("$") || context!!.startsWith("P|"))
return getContext(ctx, context)
    return if (onlyWindow) "" else getContext(ctx, "#" + context!!)
}
return s
} // 	getContext

/**
 * Get Value of Context for Window & Tab, if not found global context if available. If TabNo is
 * TAB_INFO only tab's context will be checked.
 *
 * @param ctx context
 * @param WindowNo window no
 * @param TabNo tab no
 * @param context context key
 * @return value or ""
 */
   fun getContext(ctx: Properties?, WindowNo: Int, TabNo: Int, context: String?): String? {
if (ctx == null || context == null) throw IllegalArgumentException("Require Context")
val s = ctx!!.getProperty((WindowNo).toString() + "|" + TabNo + "|" + context)
 // If TAB_INFO, don't check Window and Global context - teo_sarca BF [ 2017987 ]
    if (TAB_INFO == TabNo) return s ?: ""
 //
    return if (s.isNullOrEmpty()) getContext(ctx, WindowNo, context, false) else s
} // 	getContext

/**
 * Get Value of Context for Window & Tab, if not found global context if available. If TabNo is
 * TAB_INFO only tab's context will be checked.
 *
 * @param ctx context
 * @param WindowNo window no
 * @param TabNo tab no
 * @param context context key
 * @param onlyTab if true, no window value is searched
 * @param onlyWindow if true, no global context will be searched
 * @return value or ""
 */
  @JvmOverloads fun getContext(
      ctx: Properties?,
      WindowNo: Int,
      TabNo: Int,
      context: String?,
      onlyTab: Boolean,
      onlyWindow: Boolean = onlyTab
  ): String? {
if (ctx == null || context == null) throw IllegalArgumentException("Require Context")
val s = ctx!!.getProperty((WindowNo).toString() + "|" + TabNo + "|" + context)
 // If TAB_INFO, don't check Window and Global context - teo_sarca BF [ 2017987 ]
    if (TAB_INFO == TabNo) return s ?: ""
 //
    return if (s.isNullOrEmpty() && !onlyTab) getContext(ctx, WindowNo, context, onlyWindow) else s
} // 	getContext

/**
 * Get Context and convert it to an integer (0 if error)
 *
 * @param ctx context
 * @param context context key
 * @return value
 */
   fun getContextAsInt(ctx: Properties?, context: String?): Int {
if (ctx == null || context == null) throw IllegalArgumentException("Require Context")
var s = getContext(ctx, context)
if (s!!.length == 0) s = getContext(ctx, 0, context, false) // 	search 0 and defaults
if (s!!.length == 0) return 0
if (s == "null") return -1
 //
    try {
return Integer.parseInt(s!!)
} catch (e: NumberFormatException) {
log.error("($context) = $s", e)
}

return 0
} // 	getContextAsInt

/**
 * Get Context and convert it to an integer (0 if error)
 *
 * @param ctx context
 * @param WindowNo window no
 * @param context context key
 * @return value or 0
 */
   fun getContextAsInt(ctx: Properties, WindowNo: Int, context: String): Int {
val s = getContext(ctx, WindowNo, context, false)
if (s!!.length == 0) return 0
 //
    try {
return Integer.parseInt(s!!)
} catch (e: NumberFormatException) {
log.error("($context) = $s", e)
}

return 0
} // 	getContextAsInt

/**
 * Get Context and convert it to an integer (0 if error)
 *
 * @param ctx context
 * @param WindowNo window no
 * @param context context key
 * @param onlyWindow if true, no defaults are used unless explicitly asked for
 * @return value or 0
 */
   fun getContextAsInt(
       ctx: Properties,
       WindowNo: Int,
       context: String,
       onlyWindow: Boolean
   ): Int {
val s = getContext(ctx, WindowNo, context, onlyWindow)
if (s!!.length == 0) return 0
 //
    try {
return Integer.parseInt(s!!)
} catch (e: NumberFormatException) {
log.error("($context) = $s", e)
}

return 0
} // 	getContextAsInt

/**
 * Get Context and convert it to an integer (0 if error)
 *
 * @param ctx context
 * @param WindowNo window no
 * @param TabNo tab no
 * @param context context key
 * @return value or 0
 */
   fun getContextAsInt(ctx: Properties, WindowNo: Int, TabNo: Int, context: String): Int {
val s = getContext(ctx, WindowNo, TabNo, context)
if (s.isNullOrEmpty()) return 0
 //
    try {
return Integer.parseInt(s!!)
} catch (e: NumberFormatException) {
log.error("($context) = $s", e)
}

return 0
} // 	getContextAsInt

/**
 * Is AutoCommit
 *
 * @param ctx context
 * @return true if auto commit
 */
   fun isAutoCommit(ctx: Properties?): Boolean {
if (ctx == null) throw IllegalArgumentException("Require Context")
val s = getContext(ctx, "AutoCommit")
    return if (s != null && s == "Y") true else false
} // 	isAutoCommit

/**
 * Is Window AutoCommit (if not set use default)
 *
 * @param ctx context
 * @param WindowNo window no
 * @return true if auto commit
 */
   fun isAutoCommit(ctx: Properties?, WindowNo: Int): Boolean {
if (ctx == null) throw IllegalArgumentException("Require Context")
val s = getContext(ctx, WindowNo, "AutoCommit", false)
if (s != null) {
    return if (s == "Y")
        true
    else
        false
}
return isAutoCommit(ctx)
} // 	isAutoCommit

/**
 * Is Auto New Record
 *
 * @param ctx context
 * @return true if auto new
 */
   fun isAutoNew(ctx: Properties?): Boolean {
if (ctx == null) throw IllegalArgumentException("Require Context")
val s = getContext(ctx, "AutoNew")
    return if (s != null && s == "Y") true else false
} // 	isAutoNew

/**
 * Is Window Auto New Record (if not set use default)
 *
 * @param ctx context
 * @param WindowNo window no
 * @return true if auto new record
 */
   fun isAutoNew(ctx: Properties?, WindowNo: Int): Boolean {
if (ctx == null) throw IllegalArgumentException("Require Context")
val s = getContext(ctx, WindowNo, "AutoNew", false)
if (s != null) {
    return if (s == "Y")
        true
    else
        false
}
return isAutoNew(ctx)
} // 	isAutoNew

/**
 * Is Sales Order Trx
 *
 * @param ctx context
 * @return true if SO (default)
 */
   fun isSOTrx(ctx: Properties): Boolean {
val s = getContext(ctx, "IsSOTrx")
    return if (s != null && s == "N") false else true
} // 	isSOTrx

/**
 * Is Sales Order Trx
 *
 * @param ctx context
 * @param WindowNo window no
 * @return true if SO (default)
 */
   fun isSOTrx(ctx: Properties, WindowNo: Int): Boolean {
val s = getContext(ctx, WindowNo, "IsSOTrx", true)
    return if (s != null && s == "N") false else true
} // 	isSOTrx

/**
 * Get Context and convert it to a Timestamp if error return today's date
 *
 * @param ctx context
 * @param context context key
 * @return Timestamp
 */
   fun getContextAsDate(ctx: Properties, context: String): Timestamp? {
return getContextAsDate(ctx, 0, context)
} // 	getContextAsDate

/**
 * Get Context and convert it to a Timestamp if error return today's date
 *
 * @param ctx context
 * @param WindowNo window no
 * @param context context key
 * @return Timestamp
 */
   fun getContextAsDate(ctx: Properties?, WindowNo: Int, context: String?): Timestamp? {
if (ctx == null || context == null) throw IllegalArgumentException("Require Context")
val s = getContext(ctx, WindowNo, context, false)
 // 	JDBC Format YYYY-MM-DD	example 2000-09-11 00:00:00.0
    if (s == null || s == "") {
if (!"#date".equals(context!!, ignoreCase = true)) {
log.warn("No value for: " + context!!)
}
return Timestamp(System.currentTimeMillis())
}

 // BUG:3075946 KTU - Fix Thai Date
    /*
    //  timestamp requires time
    if (s.trim().length() == 10)
    	s = s.trim() + " 00:00:00.0";
    else if (s.indexOf('.') == -1)
    	s = s.trim() + ".0";

    return Timestamp.valueOf(s);*/

    var date: Date? = null
try {
date = timestampFormat_Default.parse(s)
} catch (e: ParseException) {
e.printStackTrace()
return null
}

    return Timestamp(date!!.time)
 // KTU
  } // 	getContextAsDate

/**
 * Get Login AD_Client_ID
 *
 * @param ctx context
 * @return login AD_Client_ID
 */
   fun getADClientID(ctx: Properties): Int {
return Env.getContextAsInt(ctx, AD_CLIENT_ID)
} // 	getADClientID

/**
 * Get Login AD_Org_ID
 *
 * @param ctx context
 * @return login AD_Org_ID
 */
   fun getAD_Org_ID(ctx: Properties): Int {
return Env.getContextAsInt(ctx, AD_ORG_ID)
} // 	getAD_Org_ID

/**
 * Get Login AD_User_ID
 *
 * @param ctx context
 * @return login AD_User_ID
 */
   fun getAD_User_ID(ctx: Properties): Int {
return Env.getContextAsInt(ctx, AD_USER_ID)
} // 	getAD_User_ID

/**
 * Get Login AD_Role_ID
 *
 * @param ctx context
 * @return login AD_Role_ID
 */
   fun getAD_Role_ID(ctx: Properties): Int {
return Env.getContextAsInt(ctx, AD_ROLE_ID)
} // 	getAD_Role_ID

/**
 * ************************************************************************ Get Preference.
 *
 * <pre>
 * 0)	Current Setting
 * 1) 	Window Preference
 * 2) 	Global Preference
 * 3)	Login settings
 * 4)	Accounting settings
</pre> *
 *
 * @param ctx context
 * @param AD_Window_ID window no
 * @param context Entity to search
 * @param system System level preferences (vs. user defined)
 * @return preference value
 */
   fun getPreference(
       ctx: Properties?,
       AD_Window_ID: Int,
       context: String?,
       system: Boolean
   ): String {
if (ctx == null || context == null) throw IllegalArgumentException("Require Context")
var retValue: String? = null
 //
    if (!system)
 // 	User Preferences
    {
retValue = ctx!!.getProperty("P$AD_Window_ID|$context") // 	Window Pref
if (retValue == null) retValue = ctx!!.getProperty("P|" + context!!) // 	Global Pref
} else
 // 	System Preferences
    {
retValue = ctx!!.getProperty("#" + context!!) // 	Login setting
if (retValue == null) retValue = ctx!!.getProperty("$" + context!!) // 	Accounting setting
}
 //
    return retValue ?: ""
} // 	getPreference

/**
 * get preference of process from env
 *
 * @param ctx
 * @param AD_Window_ID
 * @param AD_InfoWindow
 * @param AD_Process_ID_Of_Panel
 * @param context
 * @return
 */
   fun getPreference(
       ctx: Properties?,
       AD_Window_ID: Int,
       AD_InfoWindow: Int,
       AD_Process_ID_Of_Panel: Int,
       context: String?
   ): String {
if (ctx == null || context == null) throw IllegalArgumentException("Require Context")
var retValue: String? = null

retValue = ctx!!.getProperty(
        "P" +
        AD_Window_ID +
        "|" +
        AD_InfoWindow +
        "|" +
        AD_Process_ID_Of_Panel +
        "|" +
        context)

return (retValue ?: "")
} // 	getPreference

/**
 * get preference of info window from env
 *
 * @param ctx
 * @param AD_Window_ID
 * @param AD_InfoWindow
 * @param context
 * @return
 */
   fun getPreference(
       ctx: Properties?,
       AD_Window_ID: Int,
       AD_InfoWindow: Int,
       context: String?
   ): String {
if (ctx == null || context == null) throw IllegalArgumentException("Require Context")
var retValue: String? = null

retValue = ctx!!.getProperty("P$AD_Window_ID|$AD_InfoWindow|$context")

return (retValue ?: "")
} // 	getPreference

/**
 * Table is in Base Translation (AD)
 *
 * @param tableName table
 * @return true if base trl
 */
   fun isBaseTranslation(tableName: String): Boolean {
    return if (tableName.startsWith("AD") || tableName == "C_Country_Trl") true else false
} // 	isBaseTranslation

/**
 * ************************************************************************ Get Context as String
 * array with format: key == value
 *
 * @param ctx context
 * @return context string
 */
   fun getEntireContext(ctx: Properties?): Array<String?> {
if (ctx == null) throw IllegalArgumentException("Require Context")
val keyIterator = ctx!!.keys.iterator()
val sList = arrayOfNulls<String>(ctx!!.size)
var i = 0
while (keyIterator.hasNext()) {
val key = keyIterator.next()
sList[i++] = key.toString() + " == " + ctx!![key].toString()
}

return sList
} // 	getEntireContext

/**
 * Get Header info (connection, org, user)
 *
 * @param ctx context
 * @param WindowNo window
 * @return Header String
 */
   fun getHeader(ctx: Properties, WindowNo: Int): String {
val sb = StringBuilder()
if (WindowNo > 0) {
sb.append(getContext(ctx, WindowNo, "_WinInfo_WindowName", false)).append("  ")
val documentNo = getContext(ctx, WindowNo, "DocumentNo", false)
val value = getContext(ctx, WindowNo, "Value", false)
val name = getContext(ctx, WindowNo, "Name", false)
if ("" != documentNo) {
sb.append(documentNo).append("  ")
}
if ("" != value) {
sb.append(value).append("  ")
}
if ("" != name) {
sb.append(name).append("  ")
}
}
sb.append(getContext(ctx, "#AD_User_Name"))
.append("@")
.append(getContext(ctx, "#AD_Client_Name"))
.append(".")
.append(getContext(ctx, "#AD_Org_Name"))
return sb.toString()
} // 	getHeader

/**
 * Clean up context for Window Tab (i.e. delete it). Please note that this method is not clearing
 * the tab info context (i.e. _TabInfo).
 *
 * @param ctx context
 * @param WindowNo window
 * @param TabNo tab
 */
   fun clearTabContext(ctx: Properties?, WindowNo: Int, TabNo: Int) {
if (ctx == null) throw IllegalArgumentException("Require Context")
 //
    val keys = ctx!!.keys.toTypedArray()
for (i in keys.indices) {
val tag = keys[i].toString()
if ((tag.startsWith((WindowNo).toString() + "|" + TabNo + "|") && !tag.startsWith((WindowNo).toString() + "|" + TabNo + "|_TabInfo"))) {
ctx!!.remove(keys[i])
}
}
}

/**
 * Clean up all context (i.e. delete it)
 *
 * @param ctx context
 */
   fun clearContext(ctx: Properties?) {
if (ctx == null) throw IllegalArgumentException("Require Context")
ctx!!.clear()
} // 	clearContext

/**
 * Parse Context replaces global or Window context @tag@ with actual value.
 *
 * @tag@ are ignored otherwise "" is returned
 * @param ctx context
 * @param WindowNo Number of Window
 * @param value Message to be parsed
 * @param onlyWindow if true, no defaults are used
 * @param ignoreUnparsable if true, unsuccessful @return parsed String or "" if not successful and
 * ignoreUnparsable
 * @return parsed context
 */
  @JvmOverloads fun parseContext(
      ctx: Properties,
      WindowNo: Int,
      value: String?,
      onlyWindow: Boolean,
      ignoreUnparsable: Boolean = false
  ): String {
if (value == null || value!!.length == 0) return ""

var token: String
var inStr = value!!
val outStr = StringBuilder()

var i = inStr.indexOf('@')
while (i != -1) {
outStr.append(inStr.substring(0, i)) // up to @
inStr = inStr.substring(i + 1, inStr.length) // from first @

val j = inStr.indexOf('@') // next @
if (j < 0) {
if (log.isInfoEnabled) log.info("No second tag: $inStr")
 // not context variable, add back @ and break
        outStr.append("@")
break
}

token = inStr.substring(0, j)

 // IDEMPIERE-194 Handling null context variable
      var defaultV: String? = null
val idx = token.indexOf(":") // 	or clause
if (idx >= 0) {
defaultV = token.substring(idx + 1, token.length)
token = token.substring(0, idx)
}

var ctxInfo = getContext(ctx, WindowNo, token, onlyWindow) // get context
if (ctxInfo!!.length == 0 && (token.startsWith("#") || token.startsWith("$")))
ctxInfo = getContext(ctx, token) // get global context

if (ctxInfo!!.length == 0 && defaultV != null) ctxInfo = defaultV

if (ctxInfo!!.length == 0) {
if (log.isTraceEnabled)
log.trace("No Context Win=$WindowNo for: $token")
if (!ignoreUnparsable) return "" // 	token not found
} else
outStr.append(ctxInfo) // replace context with Context

inStr = inStr.substring(j + 1, inStr.length) // from second @
i = inStr.indexOf('@')
}
outStr.append(inStr) // add the rest of the string

return outStr.toString()
} // 	parseContext

/**
 * Parse Context replaces global or Window context @tag@ with actual value.
 *
 * @tag@ are ignored otherwise "" is returned
 * @param ctx context
 * @param WindowNo Number of Window
 * @param tabNo Number of Tab
 * @param value Message to be parsed
 * @param onlyTab if true, no defaults are used
 * @param ignoreUnparsable if true, unsuccessful @return parsed String or "" if not successful and
 * ignoreUnparsable
 * @return parsed context
 */
   fun parseContext(
       ctx: Properties,
       WindowNo: Int,
       tabNo: Int,
       value: String?,
       onlyTab: Boolean,
       ignoreUnparsable: Boolean
   ): String {
if (value == null || value!!.length == 0) return ""

var token: String
var inStr = value!!
val outStr = StringBuilder()

var i = inStr.indexOf('@')
while (i != -1) {
outStr.append(inStr.substring(0, i)) // up to @
inStr = inStr.substring(i + 1, inStr.length) // from first @

val j = inStr.indexOf('@') // next @
if (j < 0) {
if (log.isInfoEnabled) log.info("No second tag: $inStr")
 // not context variable, add back @ and break
        outStr.append("@")
break
}

token = inStr.substring(0, j)

 // IDEMPIERE-194 Handling null context variable
      var defaultV: String? = null
val idx = token.indexOf(":") // 	or clause
if (idx >= 0) {
defaultV = token.substring(idx + 1, token.length)
token = token.substring(0, idx)
}

var ctxInfo = getContext(ctx, WindowNo, tabNo, token, onlyTab) // get context
if (ctxInfo!!.length == 0 && (token.startsWith("#") || token.startsWith("$")))
ctxInfo = getContext(ctx, token) // get global context

if (ctxInfo!!.length == 0 && defaultV != null) ctxInfo = defaultV

if (ctxInfo!!.length == 0) {
if (log.isTraceEnabled)
log.trace("No Context Win=$WindowNo for: $token")
if (!ignoreUnparsable) return "" // 	token not found
} else
outStr.append(ctxInfo) // replace context with Context

inStr = inStr.substring(j + 1, inStr.length) // from second @
i = inStr.indexOf('@')
}
outStr.append(inStr) // add the rest of the string

return outStr.toString()
} // 	parseContext

/**
 * Parse Context replaces global or Window context @tag@ with actual value.
 *
 * @param ctx context
 * @param WindowNo Number of Window
 * @param TabNo Number of Tab
 * @param value Message to be parsed
 * @param onlyWindow if true, no defaults are used
 * @return parsed String or "" if not successful
 */
   fun parseContext(
       ctx: Properties,
       WindowNo: Int,
       tabNo: Int,
       value: String,
       onlyWindow: Boolean
   ): String {
return parseContext(ctx, WindowNo, tabNo, value, onlyWindow, false)
} // 	parseContext

/** Clean up all context (i.e. delete it)  */
   fun clearContext() {
ctx?.clear()
} // 	clearContext

/**
 * Get Graphics of container or its parent. The element may not have a Graphic if not displayed
 * yet, but the parent might have.
 *
 * @param container Container
 * @return Graphics of container or null
 */
   fun getGraphics(container: Container): Graphics? {
var element: Container? = container
while (element != null) {
val g = element!!.graphics
if (g != null) return g
element = element!!.parent
}
return null
} // 	getFrame

/**
 * Return JDialog or JFrame Parent
 *
 * @param container Container
 * @return JDialog or JFrame of container
 */
   fun getParent(container: Container): Window? {
var element: Container? = container
while (element != null) {
if (element is JDialog || element is JFrame) return element as Window?
if (element is Window) return element as Window?
element = element!!.parent
}
return null
} //  getParent

/**
 * ************************************************************************* Start Browser
 *
 * @param url url
 */
   fun startBrowser(url: String) {
if (log.isInfoEnabled) log.info(url)
contextProvider.showURL(url)
} //  startBrowser

/**
 * Sleep
 *
 * @param sec seconds
 */
   fun sleep(sec: Int) {
if (log.isInfoEnabled) log.info("Start - Seconds=$sec")
try {
Thread.sleep((sec * 1000).toLong())
} catch (e: Exception) {
log.warn("", e)
}

if (log.isInfoEnabled) log.info("End")
} // 	sleep

/**
 * Prepare the context for calling remote server (for e.g, ejb), only default and global variables
 * are pass over. It is too expensive and also can have serialization issue if every remote call
 * to server is passing the whole client context.
 *
 * @param ctx
 * @return Properties
 */
   fun getRemoteCallCtx(ctx: Properties): Properties {
val p = Properties()
val keys = ctx.keys
for (key in keys) {
if (key !is String) continue

val value = ctx[key]
if (value !is String) continue

    p[key] = value
}

return p
}
}