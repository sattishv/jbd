/* 
* @Author: Beinan
* @Date:   2014-11-06 21:25:10
* @Last Modified by:   Beinan
* @Last Modified time: 2014-11-14 21:48:42
*/
package edu.syr.jbd.tracing

import java.lang.management._

import reactivemongo.bson._
    
import edu.syr.jbd.tracing.mongo.MongoDB

object JBDTrace{
  val local = new JBDLocal
  val jvm_name = ManagementFactory.getRuntimeMXBean().getName()
  
  import scala.concurrent.ExecutionContext.Implicits.global

  def traceMethodEnter(method_desc:String):Long = {
    val invocation_id = local.get.count
    val doc = BSONDocument(
      "jvm_name" -> jvm_name,
      "thread_id" -> Thread.currentThread().getId(),
      "invocation_id" -> invocation_id,
      "method_desc" -> method_desc,
      "msg_type" -> "method_enter",
      "created_datetime" -> BSONDateTime(System.currentTimeMillis))
    
    MongoDB.coll("trace").insert(doc)   

    return invocation_id 
  }

  def traceMethodArgument(invocation_id:Long, method_desc:String, arg_seq:Int,arg_val: AnyRef) {
    val doc = BSONDocument(
      "jvm_name" -> jvm_name,
      "thread_id" -> Thread.currentThread().getId(),
      "invocation_id" -> invocation_id,
      "value" -> String.valueOf(arg_val),
      "arg_seq" -> arg_seq,
      "method_desc" -> method_desc,
      "msg_type" -> "method_argument",
      "created_datetime" -> BSONDateTime(System.currentTimeMillis))
    
    MongoDB.coll("trace").insert(doc)   


  }

  def traceMethodInvocation(method_desc: String, parent_invocation_id:Long){
    val invocation_id = local.get.count    
    val doc = BSONDocument(
      "jvm_name" -> jvm_name,
      "thread_id" -> Thread.currentThread().getId(),
      "invocation_id" -> invocation_id,
      "parent_invocation_id" -> parent_invocation_id,
      "method_desc" -> method_desc,
      "msg_type" -> "method_invoke",
      "created_datetime" -> BSONDateTime(System.currentTimeMillis))
    
    MongoDB.coll("trace").insert(doc)   
  }

  def traceReturnValue(return_val: AnyRef, method_desc: String, parent_invocation_id:Long){
    val invocation_id = local.get.count    
    val doc = BSONDocument(
      "jvm_name" -> jvm_name,
      "thread_id" -> Thread.currentThread().getId(),
      "invocation_id" -> invocation_id,
      "parent_invocation_id" -> parent_invocation_id,
      "value" -> String.valueOf(return_val),
      "method_desc" -> method_desc,
      "msg_type" -> "method_return",
      "created_datetime" -> BSONDateTime(System.currentTimeMillis))
    
    MongoDB.coll("trace").insert(doc)   

  }

  def traceFieldGetter(counter:JBDLocalValue, value: AnyRef, field:String){
    val invocation_id = local.get.count
    val doc = BSONDocument(
      "jvm_name" -> jvm_name,
      "thread_id" -> Thread.currentThread().getId(),
      "value" -> String.valueOf(value),
      "invocation_id" -> invocation_id,
      "version" -> counter.get,
      "field" -> field,
      "msg_type" -> "field_getter",      
      "created_datetime" -> BSONDateTime(System.currentTimeMillis))
    
    MongoDB.coll("trace").insert(doc)   
  }

  def traceFieldSetter(counter:JBDLocalValue, value: AnyRef, field:String){
    val invocation_id = local.get.count
    val doc = BSONDocument(
      "jvm_name" -> jvm_name,
      "thread_id" -> Thread.currentThread().getId(),
      "value" -> String.valueOf(value),
      "invocation_id" -> invocation_id,
      "version" -> counter.count,
      "field" -> field,
      "msg_type" -> "field_setter",      
      "created_datetime" -> BSONDateTime(System.currentTimeMillis))
    
    MongoDB.coll("trace").insert(doc)   
  }

  def trace(v: AnyRef):Unit = {
    //println("aaaabbbb" + v)
  }
}

class JBDLocal extends ThreadLocal[JBDLocalValue]{
  override def initialValue = new JBDLocalValue
}

class JBDLocalValue{
  var counter:Long = 0
  def count = {
    counter = counter + 1
    counter
  }
  def get = counter
}

object JBDLocalValue{
  def instance = new JBDLocalValue
}

