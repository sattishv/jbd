/* 
* @Author: troya
* @Date:   2014-11-06 17:19:37
* @Last Modified by:   Beinan
* @Last Modified time: 2014-11-06 22:20:31
*/
package edu.syr.jbd.tracing.weaving

import org.objectweb.asm._
import org.objectweb.asm.commons.AdviceAdapter

class TraceMethodAdapter(api : Int, mv : MethodVisitor, owner: String,
  access : Int, name : String, desc : String) 
extends AdviceAdapter(api, mv, access, name, desc) with Opcodes {

  lazy val is_static = (access | Opcodes.ACC_STATIC) != 0
  
  val method_key = owner + "#" + name + desc
  /**
   * tracing method invocation with the values of parameters
   */
  override def onMethodEnter() {
    val types = Type.getArgumentTypes(desc)
    val off = if(is_static) 0 else 1  //local var 0 is "this" for non-static method

    push(method_key) 
    
    //traceMethodEnter will return an invocation id
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "edu/syr/jbd/tracing/JBDTrace", 
      "traceMethodEnter", "(Ljava/lang/String;)J")

    types.foldLeft[Int](off){ //trace each argument of the current method invocation
      (pos:Int, a_type:Type) => 
        dup2() //for invocation id
        push(method_key)
        push(pos) //for argument seq
        //val a_type = types(i) //each argument type
        val opcode = a_type.getOpcode(Opcodes.ILOAD)      
        
        mv.visitVarInsn(opcode, pos)
        
        if(a_type.getSort() <= 8){ //primitive type, do boxing
          box(a_type)
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "edu/syr/jbd/tracing/JBDTrace", 
        "traceMethodArgument", "(JLjava/lang/String;ILjava/lang/Object;)V")//long, int, object
        
        pos + a_type.getSize()  //for double & long, size will be 2
    }
    pop2() //eat the invocation_id

  }

  /**
   * Tracing the return value of a method invocation
   *
   */
  override def visitMethodInsn(opcode : Int, owner : String, 
    name : String, desc : String,  itf : Boolean){
    super.visitMethodInsn(opcode, owner, name, desc, itf)
    val returnType = Type.getReturnType(desc)
    if (returnType != Type.VOID_TYPE) {
      if (returnType.getSize() == 2) {
        dup2()
      }else{
        dup()
      }

      if(returnType.getSort() <= 8){ //primitive type, do boxing
        box(returnType)
      }
      push(method_key) 
     
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, "edu/syr/jbd/tracing/JBDTrace", 
        "traceReturnValue", "(Ljava/lang/Object;Ljava/lang/String;)V")
      }
    }
}

