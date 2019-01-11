package com.mj.users.directive

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.immutable.Seq
import scala.util.Try

object DefaultPaginationParams {
  val default_offset_param: Int = 1
  val default_limit_param: Int = 10
}

trait PaginationDirectives {

  private implicit class RichConfigOption[T](value: Option[T]) {
    def ||(defaultValue: T) = value.getOrElse(defaultValue)
  }

  val config: Config = ConfigFactory.load("application.conf")

  private def getOrFail[T](key: String)(implicit tag: scala.reflect.ClassTag[T]): T = tag.runtimeClass match {
    case clazz if clazz == classOf[String] => config.getString(key).asInstanceOf[T]
    case clazz if clazz == classOf[Char] => config.getString(key).toCharArray()(0).asInstanceOf[T]
    case clazz if clazz == classOf[Boolean] => config.getBoolean(key).asInstanceOf[T]
    case clazz if clazz == classOf[Int] => config.getInt(key).asInstanceOf[T]
    case clazz if clazz == classOf[Long] => config.getLong(key).asInstanceOf[T]
    case clazz => throw new RuntimeException(s"Invalid property type ${clazz.getSimpleName} for key $key")
  }

  private def get[T](key: String)(implicit tag: scala.reflect.ClassTag[T]): Option[T] = Try(getOrFail(key)).toOption

  private lazy val OffsetParam = get[String]("akka.http.extensions.pagination.offset-param-name") || "offset"
  private lazy val LimitParam = get[String]("akka.http.extensions.pagination.limit-param-name") || "limit"
  private lazy val SortParam = get[String]("akka.http.extensions.pagination.sort-param-name") || "sort"

  private lazy val AscParam = get[String]("akka.http.extensions.pagination.asc-param-name") || "asc"
  private lazy val DescParam = get[String]("akka.http.extensions.pagination.desc-param-name") || "desc"

  private lazy val SortingSeparator = get[String]("akka.http.extensions.pagination.sorting-separator") || "!"
  private lazy val OrderSeparator = get[Char]("akka.http.extensions.pagination.order-separator") || ','

  private lazy val DefaultOffsetParam = get[Int]("akka.http.extensions.pagination.defaults.offset") || DefaultPaginationParams.default_offset_param
  private lazy val DefaultLimitParam = get[Int]("akka.http.extensions.pagination.defaults.limit") || DefaultPaginationParams.default_limit_param

  /**
    * Always returns a PageRequest
    * If values are passed as part of HTTP request, they are taken from it
    * If not, (default) values are read from configuration
    *
    * @return PageRequest - taken from HTTP request or from configuration defaults
    */
  def withPagination: Directive1[PageRequest] = {
    parameterMap.flatMap { params =>
      (params.get(OffsetParam).map(_.toInt), params.get(LimitParam).map(_.toInt)) match {
        case (Some(offset), Some(limit)) => provide(deserializePage(offset, limit, params.get(SortParam)))
        case (Some(offset), None) => provide(deserializePage(offset, DefaultLimitParam, params.get(SortParam)))
        case (None, Some(limit)) => provide(deserializePage(DefaultOffsetParam, limit, params.get(SortParam)))
        case (_, _) => provide(deserializePage(DefaultOffsetParam, DefaultLimitParam, params.get(SortParam)))
      }
    }
  }

  private def deserializePage(offset: Int, limit: Int, sorting: Option[String]) = {

    val sortingParam = sorting.map(_.split(SortingSeparator).map(_.span(_ != OrderSeparator)).collect {
      case (field, sort) if sort == ',' + AscParam => (field, Order.Asc)
      case (field, sort) if sort == ',' + DescParam => (field, Order.Desc)
    }.toMap)

    PageRequest(offset, limit, sortingParam.getOrElse(Map.empty))
  }

}

sealed trait Order

object Order {

  case object Asc extends Order

  case object Desc extends Order

}

case class PageRequest(offset: Int, limit: Int, sort: Map[String, Order])

object PageRequest {
  def default = PageRequest(DefaultPaginationParams.default_offset_param, DefaultPaginationParams.default_limit_param, Map.empty[String, Order])
}

case class PageResponse[T](elements: Seq[T], totalElements: Int)
