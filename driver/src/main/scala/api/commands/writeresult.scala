package reactivemongo.api.commands

import scala.util.control.NoStackTrace

import reactivemongo.bson.BSONValue

import reactivemongo.core.errors.DatabaseException

sealed trait WriteResult {
  def ok: Boolean
  def n: Int
  def writeErrors: Seq[WriteError]
  def writeConcernError: Option[WriteConcernError]

  /** The result code */
  def code: Option[Int]

  /** If the result is a failure, the error message */
  def errmsg: Option[String]

  def hasErrors: Boolean = !writeErrors.isEmpty || !writeConcernError.isEmpty
  def inError: Boolean = !ok || code.isDefined

  /** Returns either the [[errmsg]] or `<none>`. */
  def message = errmsg.getOrElse("<none>")

  //override
  def originalDocument = Option.empty[reactivemongo.bson.BSONDocument] // TODO
  //def stringify: String = toString + " [inError: " + inError + "]"
  //override def getMessage() = toString + " [inError: " + inError + "]"
}

object WriteResult {
  def lastError(result: WriteResult): Option[LastError] = result match {
    case error @ LastError(_, _, _, _, _, _, _, _, _, _, _, _, _, _) => Some(error)
    case _ if (result.ok) => None
    case _ => Some(LastError(
      false, // ok
      result.errmsg,
      result.code,
      None, // lastOp
      result.n,
      None, // singleShard
      false, // updatedExisting,
      None, // upserted
      None, // wnote
      false, // wtimeout,
      None, // waited,
      None, // wtime,
      result.writeErrors,
      result.writeConcernError))
  }
}

case class LastError(
  ok: Boolean,
  errmsg: Option[String],
  code: Option[Int],
  lastOp: Option[Long],
  n: Int,
  singleShard: Option[String], // string?
  updatedExisting: Boolean,
  upserted: Option[BSONValue],
  wnote: Option[WriteConcern.W],
  wtimeout: Boolean,
  waited: Option[Int],
  wtime: Option[Int],
  writeErrors: Seq[WriteError] = Nil,
  writeConcernError: Option[WriteConcernError] = None)
    extends DatabaseException with WriteResult with NoStackTrace {

  @deprecated("Use [[errmsg]]", "0.12.0")
  val err = errmsg

  override def inError: Boolean = !ok || errmsg.isDefined
  //def stringify: String = toString + " [inError: " + inError + "]"
}

/**
 * @param code the error code
 * @param errmsg the error message
 */
case class WriteError(
  index: Int,
  code: Int,
  errmsg: String)

/**
 * @param code the error code
 * @param errmsg the error message
 */
case class WriteConcernError(code: Int, errmsg: String)

case class DefaultWriteResult(
    ok: Boolean,
    n: Int,
    writeErrors: Seq[WriteError],
    writeConcernError: Option[WriteConcernError],
    code: Option[Int],
    errmsg: Option[String]) extends WriteResult {
  def flatten = writeErrors.headOption.fold(this) { firstError =>
    DefaultWriteResult(
      ok = false,
      n = n,
      writeErrors = writeErrors,
      writeConcernError = writeConcernError,
      code = code.orElse(Some(firstError.code)),
      errmsg = errmsg.orElse(Some(firstError.errmsg)))
  }
}

case class Upserted(index: Int, _id: BSONValue)

case class UpdateWriteResult(
    ok: Boolean,
    n: Int,
    nModified: Int,
    upserted: Seq[Upserted],
    writeErrors: Seq[WriteError],
    writeConcernError: Option[WriteConcernError],
    code: Option[Int],
    errmsg: Option[String]) extends WriteResult {
  def flatten = writeErrors.headOption.fold(this) { firstError =>
    UpdateWriteResult(
      ok = false,
      n = n,
      nModified = nModified,
      upserted = upserted,
      writeErrors = writeErrors,
      writeConcernError = writeConcernError,
      code = code.orElse(Some(firstError.code)),
      errmsg = errmsg.orElse(Some(firstError.errmsg)))
  }
}