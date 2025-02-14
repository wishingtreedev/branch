package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Json.{JsonArray, JsonObject, JsonString}
import dev.wishingtree.branch.macaroni.meta.Summons.summonHigherListOf

import java.time.Instant
import scala.compiletime.*
import scala.deriving.Mirror

/** A type-class for encoding values to JSON
  * @tparam A
  *   the type of the value to encode
  */
trait JsonEncoder[-A] {

  /** Encodes the value to JSON
    * @param a
    *   the value to encode
    * @return
    *   the encoded JSON value
    */
  def encode(a: A): Json

  /** Contramap the encoder to a new JsonEncoder
    * @param f
    *   the function to map the value before encoding
    * @tparam B
    *   the new type to map from
    * @return
    *   a new JsonEncoder for the mapped type
    */
  def contraMap[B](f: B => A): JsonEncoder[B] =
    a => encode(f(a))

  extension [B](b: B) {

    /** Encodes the value to JSON using the given encoder
      * @param encoder
      *   the JsonEncoder for the type
      * @return
      *   the encoded JSON value
      */
    def toJson(using encoder: JsonEncoder[B]): Json =
      encoder.encode(b)

    /** Encodes the value to a JSON string using the given encoder
      * @param encoder
      *   the JsonEncoder for the type
      * @return
      *   the encoded JSON string
      */
    def toJsonString(using encoder: JsonEncoder[B]): String =
      encoder.encode(b).toString
  }

}

/** A collection of default JsonEncoders
  */
object JsonEncoder {

  /** A JsonEncoder for Json
    */
  given JsonEncoder[Json] with {
    def encode(a: Json): Json = a
  }

  /** A JsonEncoder for JsonObject
    */
  given JsonEncoder[JsonObject] with {
    def encode(a: JsonObject): Json = a
  }

  /** A JsonEncoder for Strings
    */
  given JsonEncoder[String] with {
    def encode(a: String): Json = Json.JsonString(a)
  }

  /** A JsonEncoder for Doubles
    */
  given JsonEncoder[Double] with {
    def encode(a: Double): Json = Json.JsonNumber(a)
  }

  /** A JsonEncoder for Booleans
    */
  given JsonEncoder[Boolean] with {
    def encode(a: Boolean): Json = Json.JsonBool(a)
  }

  /** A JsonEncoder for JSON Arrays
    */
  given JsonEncoder[IndexedSeq[Json]] with {
    def encode(a: IndexedSeq[Json]): Json = Json.JsonArray(a)
  }

  /** A JsonEncoder for JSON Objects / Map[String, Json]
    */
  given JsonEncoder[Map[String, Json]] with {
    def encode(a: Map[String, Json]): Json = Json.JsonObject(a)
  }

  /** A JsonEncoder for Ints
    */
  given JsonEncoder[Int] with {
    def encode(a: Int): Json = Json.JsonNumber(a.toDouble)
  }

  /** A JsonEncoder for Longs
    */
  given JsonEncoder[Long] with {
    def encode(a: Long): Json = Json.JsonNumber(a.toDouble)
  }

  /** A JsonEncoder for Instants
    */
  given JsonEncoder[Instant] with {
    def encode(a: Instant): Json = JsonString(a.toString)
  }

  /** A JsonEncoder for BigDecimals
    */
  given JsonEncoder[BigDecimal] with {
    def encode(a: BigDecimal): Json = Json.JsonNumber(a.doubleValue)
  }

  /** Helper method for collection/iterable JsonEncoders
    * @param jsonEncoder
    *   the JsonEncoder for the element type
    * @tparam A
    *   the element type
    * @tparam F
    *   the iterable type
    * @return
    *   a new JsonEncoder for the iterable type
    */
  private[friday] def iterableEncoder[A, F[X] <: Iterable[X]](using
      jsonEncoder: JsonEncoder[A]
  ): JsonEncoder[F[A]] =
    (a: F[A]) => JsonArray(a.iterator.map(jsonEncoder.encode).toIndexedSeq)

  /** A JsonEncoder for Seqs
    * @tparam A
    *   the element type
    * @return
    *   a new JsonEncoder for Seqs
    */
  implicit def seqEncoder[A: JsonEncoder]: JsonEncoder[Seq[A]] =
    iterableEncoder[A, Seq]

  /** A JsonEncoder for Lists
    * @tparam A
    *   the element type
    * @return
    *   a new JsonEncoder for Lists
    */
  implicit def listEncoder[A: JsonEncoder]: JsonEncoder[List[A]] =
    iterableEncoder[A, List]

  /** A JsonEncoder for IndexedSeqs
    * @tparam A
    *   the element type
    * @return
    *   a new JsonEncoder for IndexedSeqs
    */
  implicit def indexedSeqEncoder[A: JsonEncoder]: JsonEncoder[IndexedSeq[A]] =
    iterableEncoder[A, IndexedSeq]

  /** A JsonEncoder for Sets
    * @tparam A
    *   the element type
    * @return
    *   a new JsonEncoder for Sets
    */
  implicit def setEncoder[A: JsonEncoder]: JsonEncoder[Set[A]] =
    iterableEncoder[A, Set]

  /** A JsonEncoder for Vectors
    * @tparam A
    *   the element type
    * @return
    *   a new JsonEncoder for Vectors
    */
  implicit def vectorEncoder[A: JsonEncoder]: JsonEncoder[Vector[A]] =
    iterableEncoder[A, Vector]

  protected class DerivedJsonEncoder[A](using encoders: List[JsonEncoder[?]])
      extends JsonEncoder[A] {
    def encode(a: A): Json = {
      val jsLabels = a.asInstanceOf[Product].productElementNames
      val jsValues = a.asInstanceOf[Product].productIterator

      val js = jsLabels
        .zip(jsValues.zip(encoders))
        .map { case (label, (value, encoder)) =>
          label -> encoder.asInstanceOf[JsonEncoder[Any]].encode(value)
        }
      JsonObject(js.toMap)
    }
  }

  inline given derived[A](using m: Mirror.Of[A]): JsonEncoder[A] = {
    inline m match {
      case s: Mirror.SumOf[A]     =>
        error("Auto derivation of Sum types is not currently supported")
      case p: Mirror.ProductOf[A] =>
        new DerivedJsonEncoder[A](using
          summonHigherListOf[p.MirroredElemTypes, JsonEncoder]
        )
    }
  }

  /** Encodes a value to JSON using the given encoder
    * @param t
    *   the value to encode
    * @param encoder
    *   the JsonEncoder for the type
    * @tparam T
    *   the type of the value
    * @return
    *   the encoded JSON value
    */
  inline def encode[T](t: T)(using encoder: JsonEncoder[T]): Json =
    encoder.encode(t)

  def from[A](f: A => Json): JsonEncoder[A] = (a: A) => f(a)
}
