package cats
package mtl

/**
  *
  * `ApplicativeAsk[F, E]` denotes the availability of a single
  * `E` value in the `F[_]` context.
  *
  * Intuitively, this means that the `E` value is an input to the entire `F[_]`
  * computation.
  *
  * `ApplicativeAsk[F, E]` has two external laws:
  * {{{
  * def askAddsNoEffects[A](fa: F[A]) = {
  *   (ask *> fa) <-> fa
  * }
  * }}}
  *
  * `ApplicativeAsk[F, E]` has one internal law:
  * {{{
  * def readerIsAskAndMap[A](f: E => A) = {
  *   ask.map(f) <-> reader(f)
  * }
  * }}}
  *
  */
trait ApplicativeAsk[F[_], E] extends Serializable {
  val applicative: Applicative[F]

  def ask: F[E]

  def reader[A](f: E => A): F[A]
}

object ApplicativeAsk {

  def apply[F[_], E](implicit applicativeAsk: ApplicativeAsk[F, E]): ApplicativeAsk[F, E] = applicativeAsk

  def ask[F[_], E](implicit ask: ApplicativeAsk[F, E]): F[E] = {
    ask.ask
  }

  def askE[E]: askEPartiallyApplied[E] = new askEPartiallyApplied[E]

  def askF[F[_]]: askFPartiallyApplied[F] = new askFPartiallyApplied[F]

  @inline final private[mtl] class askEPartiallyApplied[E](val dummy: Boolean = false) extends AnyVal {
    @inline def apply[F[_]]()(implicit ask: ApplicativeAsk[F, E]): F[E] = {
      ask.ask
    }
  }

  @inline final private[mtl] class askFPartiallyApplied[F[_]](val dummy: Boolean = false) extends AnyVal {
    @inline def apply[E]()(implicit ask: `ApplicativeAsk`[F, E]): F[E] = {
      ask.ask
    }
  }

  @inline final private[mtl] class readerFEPartiallyApplied[F[_], E](val dummy: Boolean = false) extends AnyVal {
    @inline def apply[A](f: E => A)(implicit ask: ApplicativeAsk[F, E]): F[A] = {
      ask.reader(f)
    }
  }

  def readerFE[F[_], E]: readerFEPartiallyApplied[F, E] = new readerFEPartiallyApplied[F, E]

  def reader[F[_], E, A](fun: E => A)(implicit ask: ApplicativeAsk[F, E]): F[A] = {
    ask.reader(fun)
  }

}

