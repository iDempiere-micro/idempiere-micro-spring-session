package org.idempiere.common.util

import java.io.*
import java.util.*

/** @author hengsin
 */
/**  */
class ServerContextPropertiesWrapper : Properties() {

    /* (non-Javadoc)
   * @see java.util.Properties#setProperty(java.lang.String, java.lang.String)
   */
    override fun setProperty(key: String, value: String): Any {
        return ServerContext.currentInstance.setProperty(key, value)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#load(java.io.Reader)
   */
    @Throws(IOException::class)
    override fun load(reader: Reader) {
        ServerContext.currentInstance.load(reader)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#load(java.io.InputStream)
   */
    @Throws(IOException::class)
    override fun load(inStream: InputStream) {
        ServerContext.currentInstance.load(inStream)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#store(java.io.Writer, java.lang.String)
   */
    @Throws(IOException::class)
    override fun store(writer: Writer, comments: String) {
        ServerContext.currentInstance.store(writer, comments)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#store(java.io.OutputStream, java.lang.String)
   */
    @Throws(IOException::class)
    override fun store(out: OutputStream, comments: String) {
        ServerContext.currentInstance.store(out, comments)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#loadFromXML(java.io.InputStream)
   */
    @Throws(IOException::class, InvalidPropertiesFormatException::class)
    override fun loadFromXML(`in`: InputStream) {
        ServerContext.currentInstance.loadFromXML(`in`)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#storeToXML(java.io.OutputStream, java.lang.String)
   */
    @Throws(IOException::class)
    override fun storeToXML(os: OutputStream, comment: String) {
        ServerContext.currentInstance.storeToXML(os, comment)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#storeToXML(java.io.OutputStream, java.lang.String, java.lang.String)
   */
    @Throws(IOException::class)
    override fun storeToXML(os: OutputStream, comment: String, encoding: String) {
        ServerContext.currentInstance.storeToXML(os, comment, encoding)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#getProperty(java.lang.String)
   */
    override fun getProperty(key: String): String {
        return ServerContext.currentInstance.getProperty(key)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
   */
    override fun getProperty(key: String, defaultValue: String): String {
        return ServerContext.currentInstance.getProperty(key, defaultValue)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#propertyNames()
   */
    override fun propertyNames(): Enumeration<*> {
        return ServerContext.currentInstance.propertyNames()
    }

    /* (non-Javadoc)
   * @see java.util.Properties#stringPropertyNames()
   */
    override fun stringPropertyNames(): Set<String> {
        return ServerContext.currentInstance.stringPropertyNames()
    }

    /* (non-Javadoc)
   * @see java.util.Properties#list(java.io.PrintStream)
   */
    override fun list(out: PrintStream) {
        ServerContext.currentInstance.list(out)
    }

    /* (non-Javadoc)
   * @see java.util.Properties#list(java.io.PrintWriter)
   */
    override fun list(out: PrintWriter) {
        ServerContext.currentInstance.list(out)
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#size()
   */
    /*fun getSize(): Int {
        return ServerContext.currentInstance.size()
    }*/
    override val size: Int get() {
        return ServerContext.currentInstance.size
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#isEmpty()
   */
    override fun isEmpty(): Boolean {
        return ServerContext.currentInstance.isEmpty()
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#keys()
   */
    override fun keys(): Enumeration<Any> {
        return ServerContext.currentInstance.keys()
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#elements()
   */
    override fun elements(): Enumeration<Any> {
        return ServerContext.currentInstance.elements()
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#contains(java.lang.Object)
   */
    override fun contains(value: Any): Boolean {
        return ServerContext.currentInstance.contains(value)
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#containsValue(java.lang.Object)
   */
    override fun containsValue(value: Any): Boolean {
        return ServerContext.currentInstance.containsValue(value)
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#containsKey(java.lang.Object)
   */
    override fun containsKey(key: Any): Boolean {
        return ServerContext.currentInstance.containsKey(key)
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#get(java.lang.Object)
   */
    override fun get(key: Any): Any? {
        return ServerContext.currentInstance.get(key)
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
   */
    override fun put(key: Any, value: Any): Any? {
        return ServerContext.currentInstance.put(key, value)
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#remove(java.lang.Object)
   */
    override fun remove(key: Any): Any? {
        return ServerContext.currentInstance.remove(key)
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#putAll(java.util.Map)
   *
   */
    /*fun putAll(t: Map<out Any, Any>) {
        ServerContext.currentInstance.putAll(t)
    }*/

    /* (non-Javadoc)
   * @see java.util.Hashtable#clear()
   */
    override fun clear() {
        ServerContext.currentInstance.clear()
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#clone()
   */
    override fun clone(): Any {
        return ServerContext.currentInstance.clone()
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#toString()
   */
    override fun toString(): String {
        return ServerContext.currentInstance.toString()
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#keySet()
   */
    /*
    override fun keySet(): Set<Any> {
        return ServerContext.currentInstance.keySet()
    }*/

    /* (non-Javadoc)
   * @see java.util.Hashtable#entrySet()
   */
    /*override fun entrySet(): Set<Entry<Any, Any>> {
        return ServerContext.currentInstance.entrySet
    }*/

    /* (non-Javadoc)
   * @see java.util.Hashtable#values()
   */
    override val values: MutableCollection<Any> get() {
        return ServerContext.currentInstance.values
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#equals(java.lang.Object)
   */
    override fun equals(other: Any?): Boolean {
        return ServerContext.currentInstance.equals(other)
    }

    /* (non-Javadoc)
   * @see java.util.Hashtable#hashCode()
   */
    override fun hashCode(): Int {
        return ServerContext.currentInstance.hashCode()
    }

    companion object {

        /** generated serial id  */
        private val serialVersionUID = 4383867755398619422L
    }
}
