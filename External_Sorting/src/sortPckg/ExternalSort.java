package sortPckg;

import java.io.IOException;

import global.AttrType;
import global.TupleOrder;
import heap.Heapfile;

public class ExternalSort extends Sort {

	public ExternalSort(Heapfile f, AttrType[] in, short len_in,
			short[] str_sizes, Iterator am, int sort_fld,
			TupleOrder sort_order, int sort_fld_len, int n_pages)
			throws IOException {
		
		super(in, len_in, str_sizes, am, sort_fld, sort_order, sort_fld_len, n_pages); // SortException
	}
}
