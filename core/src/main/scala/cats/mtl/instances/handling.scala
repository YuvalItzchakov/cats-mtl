package cats
package mtl
package instances

import cats.data._
import cats.implicits._

trait HandlingInstances extends HandlingInstancesLowPriority {
  implicit def handleNEither[M[_], E](implicit M: Monad[M]): monad.Handling[EitherTC[M, E]#l, E] =
    new monad.Handling[EitherTC[M, E]#l, E] {
      val raise: monad.Raising[EitherTC[M, E]#l, E] =
        instances.raising.raiseNEither(M)

      def materialize[A](fa: EitherT[M, E, A]): EitherT[M, E, Either[E, A]] =
        EitherT(fa.value.map(_.asRight))

      def handleErrorWith[A](fa: EitherT[M, E, A])(f: PartialFunction[E, A]): EitherT[M, E, A] =
        fa.recover(f)
    }
}

trait HandlingInstancesLowPriority {
  implicit def handleNIndEither[M[_], E, Err](implicit under: monad.Handling[M, E],
                                              M: Monad[M]
                                             ): monad.Handling[CurryT[EitherTCE[Err]#l, M]#l, E] =
    new monad.Handling[CurryT[EitherTCE[Err]#l, M]#l, E] {
      val raise: monad.Raising[CurryT[EitherTCE[Err]#l, M]#l, E] =
        instances.raising.raiseNInd[EitherTC[M, Err]#l, M, E](
          instances.eithert.eitherMonadTransFunctor[M, Err],
          under.raise
        )

      def materialize[A](fa: EitherT[M, Err, A]): EitherTCE[Err]#l[M, Either[E, A]] = {
        EitherT[M, Err, E Either A](
          under.materialize(fa.value).map(e =>
            Traverse[EitherC[E]#l].sequence[EitherC[Err]#l, A](e)
          )
        )
      }

      def handleErrorWith[A](fa: EitherT[M, Err, A])(f: PartialFunction[E, A]): EitherT[M, Err, A] =
        EitherT(under.handleErrorWith(fa.value)(f.andThen(Right(_))))
    }

  implicit def handleNIndState[M[_], S, Err](implicit under: monad.Handling[M, Err],
                                             M: Monad[M]
                                            ): monad.Handling[CurryT[StateTCS[S]#l, M]#l, Err] =
    new monad.Handling[CurryT[StateTCS[S]#l, M]#l, Err] {
      val raise: monad.Raising[CurryT[StateTCS[S]#l, M]#l, Err] =
        instances.raising.raiseNInd[StateTC[M, S]#l, M, Err](
          instances.statet.stateMonadLayer[M, S],
          under.raise
        )

      def materialize[A](fa: StateT[M, S, A]): StateT[M, S, Err Either A] = {
        StateT.applyF[M, S, Err Either A](fa.runF.map { d =>
          (e: S) =>
            under.materialize(d(e)).map[(S, Err Either A)] {
              case Right((newState, a)) => (newState, Right(a))
              case Left(err) => (e, Left(err))
            }
        })
      }

      def handleErrorWith[A](fa: StateT[M, S, A])(f: PartialFunction[Err, A]): StateT[M, S, A] =
        StateT((e: S) => under.handleErrorWith(fa.run(e))(f.andThen(a => (e, a))))
    }

  implicit def handleNIndReader[M[_], Env, Err](implicit under: monad.Handling[M, Err],
                                                M: Monad[M]
                                               ): monad.Handling[CurryT[ReaderTCE[Env]#l, M]#l, Err] =
    new monad.Handling[CurryT[ReaderTCE[Env]#l, M]#l, Err] {
      val raise: monad.Raising[CurryT[ReaderTCE[Env]#l, M]#l, Err] =
        instances.raising.raiseNInd[ReaderTC[M, Env]#l, M, Err](
          instances.readert.readerMonadLayer[M, Env],
          under.raise
        )

      def materialize[A](fa: ReaderT[M, Env, A]): ReaderT[M, Env, Either[Err, A]] = {
        ReaderT(fa.run.andThen(under.materialize))
      }

      def handleErrorWith[A](fa: ReaderT[M, Env, A])(f: PartialFunction[Err, A]): ReaderT[M, Env, A] =
        ReaderT((e: Env) => under.handleErrorWith(fa.run(e))(f))
    }

  implicit def handleNIndWriter[M[_], L, Err](implicit L: Monoid[L],
                                              under: monad.Handling[M, Err],
                                              M: Monad[M]
                                             ): monad.Handling[CurryT[WriterTCL[L]#l, M]#l, Err] =
    new monad.Handling[CurryT[WriterTCL[L]#l, M]#l, Err] {
      val raise: monad.Raising[CurryT[WriterTCL[L]#l, M]#l, Err] =
        instances.raising.raiseNInd[WriterTC[M, L]#l, M, Err](
          instances.writert.writerMonadLayer[M, L],
          under.raise
        )

      def materialize[A](fa: WriterT[M, L, A]): WriterT[M, L, Either[Err, A]] = {
        WriterT(under.materialize(fa.run).map {
          case Left(err) => (L.empty, Left(err))
          case Right((e, a)) => (e, Right(a))
        })
      }

      def handleErrorWith[A](fa: WriterT[M, L, A])(f: PartialFunction[Err, A]): WriterT[M, L, A] =
        WriterT(under.handleErrorWith(fa.run)(f.andThen(a => (L.empty, a))))
    }
}

object handling extends HandlingInstances
