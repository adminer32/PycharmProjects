export interface RestResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export interface PageVO<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
}
