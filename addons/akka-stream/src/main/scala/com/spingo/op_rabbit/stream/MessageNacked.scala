package com.spingo.op_rabbit.stream

class MessageNacked(id: Long) extends Exception(s"Published message with id ${id} was nacked by the broker.")
